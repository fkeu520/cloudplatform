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

async def get_db_connection():
    pool = await get_pool()
    return await pool.acquire()
