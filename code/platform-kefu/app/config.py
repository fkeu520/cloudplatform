from pydantic_settings import BaseSettings
from typing import List

class Settings(BaseSettings):
    deepseek_api_key: str
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"

    siliconflow_api_key: str
    siliconflow_base_url: str = "https://api.siliconflow.cn/v1"
    siliconflow_embedding_model: str = "BAAI/bge-m3"
    siliconflow_embedding_dim: int = 1024

    rag_top_k: int = 5
    rag_min_score: float = 0.5

    max_file_size_mb: int = 50
    allowed_extensions: str = "pdf,docx,md,txt"

    host: str = "0.0.0.0"
    port: int = 8000

    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "platform"
    mysql_password: str = "platform123"
    mysql_database: str = "platform_kefu"

    # 2026-09-30 连接池容量与获取超时。
    # maxsize 决定并发上限: 每个 SSE 问答会同时占用一条连接, 5 太小。
    # acquire_timeout 是安全网 —— 正常情况下永远用不到; 一旦有 handler 再次泄漏连接,
    # 池耗尽时会抛 PoolExhaustedError (503) 而不是让请求永久挂起。
    # 挂起是最坏的失败模式: 容器健康检查照样 200, 界面只表现为"发送无响应"。
    mysql_pool_max_size: int = 10
    mysql_acquire_timeout: float = 10.0

    minio_endpoint: str = "platform-minio:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin123"
    minio_bucket: str = "kefu-docs"
    minio_secure: bool = False

    # 2026-09-28 安全加固: 移除了此前的硬编码 jwt_secret 默认值(与 Java JwtUtil 共用的旧密钥,
    # 已泄露于 git 历史, 本轮统一作废). 改为必填: 缺失时 pydantic-settings 启动即失败,
    # 不再静默使用已泄露密钥. 注意此处刻意不复述旧密钥字面量, 避免二次泄露.
    jwt_secret: str

    # P0 security fix (2026-09-24): internal gateway-to-kefu token contract.
    # Must match the KEFU_INTERNAL_TOKEN set on platform-gateway in docker-compose.
    # Gateway computes X-Kefu-Internal-Token = HMAC-SHA256(secret, "kefu-internal")
    # and attaches it to every request forwarded to Kefu routes.
    # KeFu rejects any X-User-* header unless this token is present and valid.
    kefu_internal_token: str = ""

    @property
    def allowed_extensions_list(self) -> List[str]:
        return [ext.strip().lower() for ext in self.allowed_extensions.split(",")]

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"

settings = Settings()
