import numpy as np
import httpx
from app.config import settings

class SiliconFlowEmbedder:
    def __init__(self):
        self.api_key = settings.siliconflow_api_key
        self.base_url = settings.siliconflow_base_url
        self.model = settings.siliconflow_embedding_model
        self.dim = settings.siliconflow_embedding_dim

    def embed_single(self, text: str) -> np.ndarray:
        return self.embed([text])[0]

    def embed(self, texts: list) -> list:
        # 2026-09-28: 空 key 时 httpx 抛的是难以定位的
        # "Illegal header value b'Bearer '" —— 先显式拦截, 给出可读原因。
        if not self.api_key or not self.api_key.strip():
            raise RuntimeError(
                "SILICONFLOW_API_KEY 未配置, 无法调用向量服务 (检查 docker-compose/.env)"
            )
        url = f"{self.base_url}/embeddings"
        headers = {
            "Authorization": f"Bearer {self.api_key.strip()}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": self.model,
            "input": texts,
            "encoding_format": "float",
        }
        with httpx.Client(timeout=60) as client:
            resp = client.post(url, json=payload, headers=headers)
            resp.raise_for_status()
            data = resp.json()

        vectors = [item["embedding"] for item in data["data"]]
        return [np.array(v, dtype=np.float32) for v in vectors]

embedder = SiliconFlowEmbedder()
