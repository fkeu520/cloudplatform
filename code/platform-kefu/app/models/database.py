import asyncio

import aiomysql
from typing import List
from app.config import settings

POOL: aiomysql.Pool = None


class PoolExhaustedError(RuntimeError):
    """获取连接超时 —— 池已被占满, 通常意味着某处 handler 泄漏了连接。"""


class _PooledConnection:
    """把 ``close()`` 重定向到 ``Pool.release()`` 的连接代理。

    为什么需要它
    ------------
    ``aiomysql.Connection.close()`` 只关 socket。连接池仍把该连接留在自己的
    ``_used`` 集合里, 直到有人调用 ``Pool.release(conn)``。于是「取一条连接 →
    try/finally 里 close()」这种写法每个请求净消耗一个池位; 池位耗尽后
    ``Pool.acquire()`` 会永久阻塞在内部条件变量上
    —— 所有碰 MySQL 的 ``/api/kefu/*`` handler 全部失去响应, 而不碰库的
    ``/api/kefu/health`` 依然返回 200, 容器在监控里始终"健康"。

    142 实测: 浏览器一轮流程
    ``POST /sessions → GET /sessions/my → GET /sessions/{sid} → GET /messages``
    正好四个请求, 所以第五个 (发送消息) 就是第一个挂起的, 现象是"点击发送无响应"。
    同版本 aiomysql 0.3.2 上复现: 第 6 次 ``pool.acquire()`` 直接超时。

    除 ``close()`` 外的所有属性都转发给真实连接, 因此现有 36 处
    ``get_db_connection()`` 调用点无需改动即自动正确; ``close()`` 做成幂等,
    因为 docs.py 的异常路径可能走到它两次 (重复 release 会触发 aiomysql 的
    ``assert conn in self._used``)。
    """

    __slots__ = ("_pool", "_conn", "_released")

    def __init__(self, pool: aiomysql.Pool, conn: aiomysql.Connection) -> None:
        self._pool = pool
        self._conn = conn
        self._released = False

    def __getattr__(self, name):
        return getattr(self._conn, name)

    def close(self) -> None:
        if self._released:
            return
        self._released = True
        # 刻意不调用 conn.close(): socket 必须保持打开才能被复用。
        self._pool.release(self._conn)

    async def __aenter__(self) -> aiomysql.Connection:
        return self._conn

    async def __aexit__(self, exc_type, exc, tb) -> None:
        self.close()

CREATE_TABLES_SQL = """
CREATE TABLE IF NOT EXISTS documents (
    doc_id VARCHAR(64) PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    upload_time VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'processing',
    chunk_count INT NOT NULL DEFAULT 0,
    file_path VARCHAR(512),
    content_hash VARCHAR(64) NULL,
    error_message VARCHAR(512) NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    INDEX idx_documents_tenant (tenant_id),
    UNIQUE INDEX uk_documents_tenant_hash (tenant_id, content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chunks (
    chunk_id VARCHAR(64) PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    doc_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    token_count INT NOT NULL DEFAULT 0,
    index_in_doc INT NOT NULL DEFAULT 0,
    vector_id INT DEFAULT NULL,
    INDEX idx_doc_id (doc_id),
    INDEX idx_chunks_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ask_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    ask_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    user_id BIGINT DEFAULT NULL,
    username VARCHAR(64) DEFAULT NULL,
    latency_ms INT DEFAULT 0,
    chunks_used TEXT,
    model VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ask_id (ask_id),
    INDEX idx_ask_logs_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2026-09-24 M7-P0 租户隔离 (platform-kefu)
-- 迁移策略:
--   1) 旧表补列时使用 tenant_id BIGINT NOT NULL DEFAULT 0
--      (kefu_session AFTER id; kefu_message AFTER msg_id).
--   2) 旧行先落在 tenant 0，普通租户查询不会返回；平台管理员仍可审计这些行。
--   3) 业务应根据 session 创建时的租户日志 / customer_id 映射批量回填旧行。
--   4) 新增 tenant 索引以支持租户级过滤。
-- 注意: CREATE TABLE IF NOT EXISTS 与 ALTER 路径保持同一列定义，避免 NULL 孤儿行。
CREATE TABLE IF NOT EXISTS kefu_session (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    customer_id BIGINT DEFAULT NULL,
    customer_name VARCHAR(255) DEFAULT NULL,
    contact VARCHAR(128) DEFAULT NULL,
    channel VARCHAR(32) DEFAULT 'web',
    status VARCHAR(32) DEFAULT 'AI',
    agent_id BIGINT DEFAULT NULL,
    agent_name VARCHAR(128) DEFAULT NULL,
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    end_time DATETIME DEFAULT NULL,
    satisfaction TINYINT DEFAULT NULL,
    satisfaction_comment VARCHAR(512) DEFAULT NULL,
    last_message_preview VARCHAR(255) DEFAULT NULL,
    last_message_at DATETIME DEFAULT NULL,
    hidden_by_customer TINYINT NOT NULL DEFAULT 0,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_channel (channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kefu_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_id VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    data_source_refs TEXT,
    knowledge_refs TEXT,
    model VARCHAR(64) DEFAULT '',
    latency_ms INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_id (msg_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_session_id (session_id),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kefu_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    faq_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(64) DEFAULT '其他',
    hit_count INT NOT NULL DEFAULT 0,
    sat_sum INT NOT NULL DEFAULT 0,
    sat_count INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_faq_id (faq_id),
    INDEX idx_faq_tenant (tenant_id),
    INDEX idx_category (category),
    INDEX idx_enabled (enabled),
    FULLTEXT INDEX ft_question (question)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kefu_data_source (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    module_ref VARCHAR(255) DEFAULT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    sync_strategy VARCHAR(32) DEFAULT 'realtime',
    sync_interval INT DEFAULT 0,
    config_json TEXT,
    intent_keywords TEXT,
    last_sync_at DATETIME DEFAULT NULL,
    last_sync_status VARCHAR(32) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled (enabled),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kefu_session_data_source_hit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    session_id VARCHAR(64) NOT NULL,
    data_source_id VARCHAR(64) NOT NULL,
    hit_count INT NOT NULL DEFAULT 1,
    last_hit_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_hit_tenant_session_source (tenant_id, session_id, data_source_id),
    INDEX idx_hit_tenant (tenant_id),
    INDEX idx_session (session_id),
    INDEX idx_source (data_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kefu_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    eval_id VARCHAR(64) NOT NULL,
    msg_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) DEFAULT NULL,
    score TINYINT DEFAULT NULL,
    comment VARCHAR(512) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_eval_id (eval_id),
    INDEX idx_eval_tenant (tenant_id),
    INDEX idx_session (session_id),
    INDEX idx_source (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2026-10 Phase-1 全量摄取: 数据源记录 + 字段展开表
CREATE TABLE IF NOT EXISTS kefu_datasource_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) NOT NULL,
    record_key VARCHAR(191) NOT NULL,
    entity_type VARCHAR(64) NOT NULL DEFAULT 'enterprise',
    entity_id VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(255) NOT NULL,
    record_json JSON NOT NULL,
    profile_text TEXT NOT NULL,
    profile_hash CHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_source_key (source_id, record_key),
    INDEX idx_record_tenant (tenant_id),
    INDEX idx_record_source_entity (source_id, entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kefu_datasource_record_field (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id VARCHAR(64) NOT NULL,
    record_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    field_key VARCHAR(64) NOT NULL,
    field_label VARCHAR(128) NOT NULL,
    field_value TEXT NOT NULL,
    field_value_norm VARCHAR(512) NOT NULL,
    searchable TINYINT NOT NULL DEFAULT 1,
    exact_matchable TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_field (record_id, field_key),
    INDEX idx_field_lookup (source_id, tenant_id, field_key, field_value_norm(255)),
    INDEX idx_field_exact (source_id, field_key, field_value_norm(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"""

SEED_DATA_SOURCES_SQL = """
INSERT IGNORE INTO kefu_data_source (id, name, type, module_ref, enabled, sync_strategy, intent_keywords, config_json) VALUES
('faq', 'FAQ 库', 'internal', NULL, 1, 'realtime', '["FAQ","常见问题","怎么","如何","多少"]', '{"table":"kefu_faq"}'),
('knowledge', '知识库', 'vector_search', NULL, 1, 'realtime', '["知识","文档","手册","流程"]', '{"table":"chunks","vector":"faiss"}'),
('park-enterprise', '企业档案', 'http_api', 'park-enterprise:8094', 1, 'realtime', '["企业","公司","入驻","客户","联系人"]', '{"endpoint":"/enterprise/page","fields":["id","name","contact","status","industry"]}'),
('park-business', '招商管理', 'http_api', 'platform-business:8080', 0, 'realtime', '["商机","跟进","客户分配","销售"]', '{"endpoint":"/api/business/opportunity/query"}'),
('park-space', '空间房源', 'http_api', 'park-space:8091', 0, 'realtime', '["房源","空房","租金","面积","户型"]', '{"endpoint":"/api/space/room/query"}'),
('park-contract', '合同', 'http_api', 'park-contract:8093', 0, 'realtime', '["合同","到期","续签","条款"]', '{"endpoint":"/api/contract/query"}');
"""

# 2026-09-30 校准 module_ref。
#
# 原始种子数据把 4 个 park-* 数据源都写成 platform-<x>:8080, 但 142 上实际的
# 容器名是 park-<x> (platform-<x> 连 DNS 都解析不了), 端口也不是 8080。
# 服务名 + 端口双重错误, 适配器就算连上了也会打错端口。
#
# 端口已逐个实测 (/actuator/health 均 200):
#   park-space 8091 / park-property 8092 / park-contract 8093 / park-enterprise 8094
#
# park-business 不在此列表: compose 里根本没有这个服务, 无从核实指向,
# 保持原值不动 (enabled=0, 暂不启用), 避免编造一个不存在的目标。
_DATA_SOURCE_REF_FIXES = (
    ("park-enterprise", "park-enterprise:8094"),
    ("park-space", "park-space:8091"),
    ("park-contract", "park-contract:8093"),
)


def init_db():
    pass


_TENANT_COLUMN_MIGRATIONS = (
    ("documents", "tenant_id BIGINT NOT NULL DEFAULT 0"),
    ("chunks", "tenant_id BIGINT NOT NULL DEFAULT 0"),
    ("ask_logs", "tenant_id BIGINT NOT NULL DEFAULT 0"),
    ("kefu_session", "tenant_id BIGINT NOT NULL DEFAULT 0 AFTER id"),
    ("kefu_message", "tenant_id BIGINT NOT NULL DEFAULT 0 AFTER msg_id"),
    ("kefu_faq", "tenant_id BIGINT NOT NULL DEFAULT 0"),
    ("kefu_session_data_source_hit", "tenant_id BIGINT NOT NULL DEFAULT 0"),
    ("kefu_evaluation", "tenant_id BIGINT NOT NULL DEFAULT 0"),
)

_TENANT_INDEX_MIGRATIONS = (
    ("documents", "idx_documents_tenant", "tenant_id"),
    ("chunks", "idx_chunks_tenant", "tenant_id"),
    ("ask_logs", "idx_ask_logs_tenant", "tenant_id"),
    ("kefu_session", "idx_tenant_id", "tenant_id"),
    ("kefu_message", "idx_tenant_id", "tenant_id"),
    ("kefu_faq", "idx_faq_tenant", "tenant_id"),
    ("kefu_session_data_source_hit", "idx_hit_tenant", "tenant_id"),
    ("kefu_evaluation", "idx_eval_tenant", "tenant_id"),
)


async def _ensure_tenant_schema(cur) -> None:
    """Apply idempotent tenant-column/index migrations to existing databases."""
    for table, definition in _TENANT_COLUMN_MIGRATIONS:
        await cur.execute(
            "SELECT COUNT(*) FROM information_schema.columns "
            "WHERE table_schema=DATABASE() AND table_name=%s AND column_name='tenant_id'",
            (table,),
        )
        if (await cur.fetchone())[0] == 0:
            await cur.execute(f"ALTER TABLE `{table}` ADD COLUMN {definition}")


# 2026-10  P1/P2: 文档异步上传 新增可空列 + 租户级唯一索引。
# 沿用 `_ensure_tenant_schema` 的信息模式检查风格 —— 不用 IF NOT EXISTS (DDL
# 不可识别), 改为显式查 information_schema 再决定 ALTER, 保证幂等。
_DOCUMENT_SCHEMA_MIGRATIONS = (
    ("documents", "content_hash VARCHAR(64) NULL"),
    ("documents", "error_message VARCHAR(512) NULL"),
    ("documents", "started_at DATETIME NULL"),
    ("documents", "finished_at DATETIME NULL"),
)

_DOCUMENT_UNIQUE_INDEX = (
    "documents",
    "uk_documents_tenant_hash",
    "(tenant_id, content_hash)",
)


async def _ensure_document_schema(cur) -> None:
    """Add content_hash/error_message/started_at/finished_at + unique index."""
    for table, definition in _DOCUMENT_SCHEMA_MIGRATIONS:
        await cur.execute(
            "SELECT COUNT(*) FROM information_schema.columns "
            "WHERE table_schema=DATABASE() AND table_name=%s AND column_name=%s",
            (table, definition.split()[0]),
        )
        if (await cur.fetchone())[0] == 0:
            await cur.execute(f"ALTER TABLE `{table}` ADD {definition}")

    await cur.execute(
        "SELECT COUNT(*) FROM information_schema.statistics "
        "WHERE table_schema=DATABASE() AND table_name=%s AND index_name=%s",
        (_DOCUMENT_UNIQUE_INDEX[0], _DOCUMENT_UNIQUE_INDEX[1]),
    )
    if (await cur.fetchone())[0] == 0:
        await cur.execute(
            f"ALTER TABLE `{_DOCUMENT_UNIQUE_INDEX[0]}` "
            f"ADD UNIQUE INDEX `{_DOCUMENT_UNIQUE_INDEX[1]}` "
            f"{_DOCUMENT_UNIQUE_INDEX[2]}"
        )

    for table, index_name, column_name in _TENANT_INDEX_MIGRATIONS:
        await cur.execute(
            "SELECT COUNT(*) FROM information_schema.statistics "
            "WHERE table_schema=DATABASE() AND table_name=%s AND index_name=%s",
            (table, index_name),
        )
        if (await cur.fetchone())[0] == 0:
            await cur.execute(
                f"ALTER TABLE `{table}` ADD INDEX `{index_name}` ({column_name})"
            )


async def _ensure_kefu_session_hidden_schema(cur) -> None:
    """幂等迁移: 旧库补 kefu_session.hidden_by_customer 列 (软隐藏, 仅属主可见性)."""
    await cur.execute(
        "SELECT COUNT(*) FROM information_schema.columns "
        "WHERE table_schema=DATABASE() AND table_name='kefu_session' "
        "AND column_name='hidden_by_customer'",
    )
    if (await cur.fetchone())[0] == 0:
        await cur.execute(
            "ALTER TABLE `kefu_session` "
            "ADD COLUMN hidden_by_customer TINYINT NOT NULL DEFAULT 0"
        )


async def _ensure_data_source_refs(cur) -> None:
    """校准已存在数据源的 module_ref。

    SEED_DATA_SOURCES_SQL 用的是 INSERT IGNORE, 只在行不存在时插入; 已经写错
    (platform-enterprise:8080) 的行不会被更新。而 module_ref 会直接显示在
    「数据源」管理页上, 不修的话运维看到的指向是错的, 后人照着排查会走偏。
    """
    for source_id, module_ref in _DATA_SOURCE_REF_FIXES:
        await cur.execute(
            "UPDATE kefu_data_source SET module_ref=%s "
            "WHERE id=%s AND module_ref<>%s",
            (module_ref, source_id, module_ref),
        )


async def get_pool():
    global POOL
    if POOL is None:
        POOL = await aiomysql.create_pool(
            host=settings.mysql_host,
            port=settings.mysql_port,
            user=settings.mysql_user,
            password=settings.mysql_password,
            db=settings.mysql_database,
            charset='utf8mb4',
            autocommit=True,
            minsize=1,
            maxsize=settings.mysql_pool_max_size,
        )
    return POOL


def _split_sql_statements(sql_text: str) -> List[str]:
    """Split a SQL script into executable statements.

    `--` line comments are stripped before splitting. The DDL constants below
    carry Chinese `--` comments, and a naive split(";") can hand MySQL a chunk
    that still has comment text glued to real SQL (a comment line whose newline
    did not survive, or a comment containing a separator). MySQL then reports a
    syntax error pointing at the comment text, e.g.

        1064 ... near 'kefu_message AFTER msg_id).\\n--   2) ...' at line 1

    which makes startup fail with a misleading error and puts the container in a
    restart loop. Removing comments first, and skipping blanks, keeps only real
    DDL/DML.
    """
    lines = []
    for line in sql_text.splitlines():
        stripped = line.lstrip()
        if stripped.startswith("--"):
            continue
        lines.append(line)
    body = "\n".join(lines)
    return [stmt.strip() for stmt in body.split(";") if stmt.strip()]


async def init_tables():
    pool = await get_pool()
    async with pool.acquire() as conn:
        async with conn.cursor() as cur:
            for stmt in _split_sql_statements(CREATE_TABLES_SQL):
                await cur.execute(stmt)
            await _ensure_tenant_schema(cur)
            await _ensure_document_schema(cur)
            await _ensure_kefu_session_hidden_schema(cur)
            for stmt in _split_sql_statements(SEED_DATA_SOURCES_SQL):
                await cur.execute(stmt)
            await _ensure_data_source_refs(cur)


async def get_db_connection() -> _PooledConnection:
    """从池中取一条连接, 交给调用方在 ``finally`` 里 ``close()``。

    ``close()`` 会把连接归还池中 (见 :class:`_PooledConnection`)。取不到连接时抛
    :class:`PoolExhaustedError` 而不是无限等待 —— 由 main.py 映射成 503。
    """
    pool = await get_pool()
    try:
        conn = await asyncio.wait_for(
            pool.acquire(), timeout=settings.mysql_acquire_timeout
        )
    except asyncio.TimeoutError as exc:
        raise PoolExhaustedError(
            f"no MySQL connection available within {settings.mysql_acquire_timeout}s "
            f"(pool maxsize={settings.mysql_pool_max_size}); a handler is most "
            f"likely leaking pooled connections"
        ) from exc
    return _PooledConnection(pool, conn)
