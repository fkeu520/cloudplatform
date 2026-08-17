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

    minio_endpoint: str = "platform-minio:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin123"
    minio_bucket: str = "kefu-docs"
    minio_secure: bool = False

    jwt_secret: str = "cloudhub-platform-secret-key-2024-change-in-production"

    @property
    def allowed_extensions_list(self) -> List[str]:
        return [ext.strip().lower() for ext in self.allowed_extensions.split(",")]

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"

settings = Settings()
