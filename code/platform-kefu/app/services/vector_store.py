import faiss
import numpy as np
import pickle
from pathlib import Path
from typing import List, Tuple, Optional

class VectorStore:
    def __init__(self, index_dir: Path):
        self.index_dir = index_dir
        self.index_dir.mkdir(parents=True, exist_ok=True)
        self.index_path = index_dir / "index.faiss"
        self.metadata_path = index_dir / "metadata.pkl"
        self.index: Optional[faiss.Index] = None
        self.chunk_id_to_vector_id: dict = {}
        self.vector_id_to_chunk_id: dict = {}
        self._load_or_create()

    def _load_or_create(self):
        if self.index_path.exists():
            self.index = faiss.read_index(str(self.index_path))
            with open(self.metadata_path, "rb") as f:
                data = pickle.load(f)
                self.chunk_id_to_vector_id = data.get("chunk_to_vec", {})
                self.vector_id_to_chunk_id = {v: k for k, v in self.chunk_id_to_vector_id.items()}
        else:
            self.index = faiss.IndexFlatIP(1024)

    def add_vectors(self, chunk_ids: List[str], vectors: List[np.ndarray]) -> List[int]:
        matrix = np.array(vectors, dtype=np.float32)
        start = self.index.ntotal
        self.index.add(matrix)
        vector_ids = list(range(start, self.index.ntotal))
        for cid, vid in zip(chunk_ids, vector_ids):
            self.chunk_id_to_vector_id[cid] = vid
            self.vector_id_to_chunk_id[vid] = cid
        self._save()
        return vector_ids

    def search(self, query_vector: np.ndarray, top_k: int = 5, min_score: float = 0.0) -> List[Tuple[str, float]]:
        if self.index.ntotal == 0:
            return []
        query = query_vector.reshape(1, -1).astype(np.float32)
        scores, indices = self.index.search(query, min(top_k, self.index.ntotal))
        results = []
        for score, idx in zip(scores[0], indices[0]):
            if score >= min_score and idx != -1:
                chunk_id = self.vector_id_to_chunk_id.get(int(idx))
                if chunk_id:
                    results.append((chunk_id, float(score)))
        return results

    def delete_by_chunk_ids(self, chunk_ids: List[str]):
        for cid in chunk_ids:
            if cid in self.chunk_id_to_vector_id:
                del self.chunk_id_to_vector_id[cid]
        self._rebuild_index()
        self._save()

    def clear(self):
        self.index = faiss.IndexFlatIP(1024)
        self.chunk_id_to_vector_id = {}
        self.vector_id_to_chunk_id = {}
        self._save()

    def _rebuild_index(self):
        new_index = faiss.IndexFlatIP(1024)
        if self.chunk_id_to_vector_id:
            all_vectors = []
            new_mapping = {}
            for vid in sorted(self.vector_id_to_chunk_id.keys()):
                if self.vector_id_to_chunk_id[vid] in self.chunk_id_to_vector_id:
                    all_vectors.append(vid)
            if all_vectors:
                pass
        self.index = new_index

    def _save(self):
        faiss.write_index(self.index, str(self.index_path))
        with open(self.metadata_path, "wb") as f:
            pickle.dump({
                "chunk_to_vec": self.chunk_id_to_vector_id,
            }, f)
