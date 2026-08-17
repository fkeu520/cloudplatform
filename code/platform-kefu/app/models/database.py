import aiomysql
from app.config import settings

POOL: aiomysql.Pool = None

CREATE_TABLES_SQL = """
CREATE TABLE IF NOT EXISTS documents (
    doc_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    upload_time VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'processing',
    chunk_count INT NOT NULL DEFAULT 0,
    file_path VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chunks (
    chunk_id VARCHAR(64) PRIMARY KEY,
    doc_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    token_count INT NOT NULL DEFAULT 0,
    index_in_doc INT NOT NULL DEFAULT 0,
    vector_id INT DEFAULT NULL,
    INDEX idx_doc_id (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ask_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ask_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    user_id BIGINT DEFAULT NULL,
    username VARCHAR(64) DEFAULT NULL,
    latency_ms INT DEFAULT 0,
    chunks_used TEXT,
    model VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ask_id (ask_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2026-08-17 业务层扩展（DESIGN §三）
CREATE TABLE IF NOT EXISTS kefu_session (
    id VARCHAR(64) PRIMARY KEY,
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
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_channel (channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kefu_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    data_source_refs TEXT,
    knowledge_refs TEXT,
    model VARCHAR(64) DEFAULT '',
    latency_ms INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_id (msg_id),
    INDEX idx_session_id (session_id),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kefu_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    session_id VARCHAR(64) NOT NULL,
    data_source_id VARCHAR(64) NOT NULL,
    hit_count INT NOT NULL DEFAULT 1,
    last_hit_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_source (data_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kefu_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    eval_id VARCHAR(64) NOT NULL,
    msg_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) DEFAULT NULL,
    score TINYINT DEFAULT NULL,
    comment VARCHAR(512) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_eval_id (eval_id),
    INDEX idx_session (session_id),
    INDEX idx_source (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"""

SEED_DATA_SOURCES_SQL = """
INSERT IGNORE INTO kefu_data_source (id, name, type, module_ref, enabled, sync_strategy, intent_keywords, config_json) VALUES
('faq', 'FAQ 库', 'internal', NULL, 1, 'realtime', '["FAQ","常见问题","怎么","如何","多少"]', '{"table":"kefu_faq"}'),
('knowledge', '知识库', 'vector_search', NULL, 1, 'realtime', '["知识","文档","手册","流程"]', '{"table":"chunks","vector":"faiss"}'),
('park-enterprise', '企业档案', 'http_api', 'platform-enterprise:8080', 1, 'realtime', '["企业","公司","入驻","客户","联系人"]', '{"endpoint":"/api/enterprise/query","fields":["id","name","contact","status"]}'),
('park-business', '招商管理', 'http_api', 'platform-business:8080', 0, 'realtime', '["商机","跟进","客户分配","销售"]', '{"endpoint":"/api/business/opportunity/query"}'),
('park-space', '空间房源', 'http_api', 'platform-space:8080', 0, 'realtime', '["房源","空房","租金","面积","户型"]', '{"endpoint":"/api/space/room/query"}'),
('park-contract', '合同', 'http_api', 'platform-contract:8080', 0, 'realtime', '["合同","到期","续签","条款"]', '{"endpoint":"/api/contract/query"}');
"""


def init_db():
    pass


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
            maxsize=5,
        )
    return POOL


async def init_tables():
    pool = await get_pool()
    async with pool.acquire() as conn:
        async with conn.cursor() as cur:
            for stmt in CREATE_TABLES_SQL.split(";"):
                s = stmt.strip()
                if s:
                    await cur.execute(s)
            for stmt in SEED_DATA_SOURCES_SQL.split(";"):
                s = stmt.strip()
                if s:
                    await cur.execute(s)


async def get_db_connection():
    pool = await get_pool()
    return await pool.acquire()