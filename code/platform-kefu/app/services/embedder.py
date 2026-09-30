import numpy as np
import httpx
from app.config import settings


class SiliconFlowEmbedder:
    # 单次请求最多送多少条。142 实测 128 条 x 1001 字符正常,
    # 256 条 x 511 字符也正常 —— 瓶颈是单条长度, 不是条数。
    BATCH_SIZE = 32

    def __init__(self):
        self.api_key = settings.siliconflow_api_key
        self.base_url = settings.siliconflow_base_url
        self.model = settings.siliconflow_embedding_model
        self.dim = settings.siliconflow_embedding_dim

    def embed_single(self, text: str) -> np.ndarray:
        return self.embed([text])[0]

    def embed(self, texts: list) -> list:
        """批量向量化。

        2026-09-30: 分批发送 + 可定位的错误信息。
        旧实现把整个文档的所有块一次塞进一个请求, 且 ``raise_for_status()``
        直接把 SiliconFlow 的 400 抛出去, 上传接口只看到
        "Client error '400 Bad Request' for url '.../embeddings'" ——
        既不知道是哪一块越界, 也不知道真实原因 (BAAI/bge-m3 单条 8192 token)。
        """
        if not self.api_key or not self.api_key.strip():
            raise RuntimeError(
                "SILICONFLOW_API_KEY 未配置, 无法调用向量服务 (检查 docker-compose/.env)"
            )
        if not texts:
            return []

        url = f"{self.base_url}/embeddings"
        headers = {
            "Authorization": f"Bearer {self.api_key.strip()}",
            "Content-Type": "application/json",
        }

        vectors: list = []
        for start in range(0, len(texts), self.BATCH_SIZE):
            batch = texts[start:start + self.BATCH_SIZE]
            payload = {
                "model": self.model,
                "input": batch,
                "encoding_format": "float",
            }
            try:
                with httpx.Client(timeout=120) as client:
                    resp = client.post(url, json=payload, headers=headers)
                    resp.raise_for_status()
                    data = resp.json()
            except httpx.HTTPStatusError as exc:
                # 带上定位信息: 批次号 + 该批最长的几条的长度
                longest = sorted((len(t) for t in batch), reverse=True)[:3]
                detail = ""
                try:
                    detail = exc.response.text[:300]
                except Exception:
                    pass
                raise RuntimeError(
                    f"向量服务返回 {exc.response.status_code} "
                    f"(第 {start // self.BATCH_SIZE + 1} 批, 共 {len(batch)} 条, "
                    f"最长字符数 {longest}): {detail or exc}"
                ) from exc
            except httpx.HTTPError as exc:
                raise RuntimeError(f"调用向量服务失败: {exc}") from exc

            items = data.get("data") or []
            if len(items) != len(batch):
                raise RuntimeError(
                    f"向量服务返回条数不匹配: 期望 {len(batch)}, 实际 {len(items)}"
                )
            vectors.extend(
                np.array(item["embedding"], dtype=np.float32) for item in items
            )

        return vectors


embedder = SiliconFlowEmbedder()
