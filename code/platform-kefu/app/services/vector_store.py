import faiss
import numpy as np
import pickle
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

_store_cache: Dict[Path, "VectorStore"] = {}
# Track all instances for cross-instance reload notification
_all_instances: Set["VectorStore"] = set()


class VectorStore:
    def __init__(self, index_dir: Path):
        self.index_dir = Path(index_dir)
        self.index_dir.mkdir(parents=True, exist_ok=True)
        self.index_path = self.index_dir / "index.faiss"
        self.metadata_path = self.index_dir / "metadata.pkl"
        self.index: Optional[faiss.Index] = None
        self.chunk_id_to_vector_id: dict = {}
        self.vector_id_to_chunk_id: dict = {}
        _all_instances.add(self)
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
            vector_id = self.chunk_id_to_vector_id.pop(cid, None)
            if vector_id is not None:
                self.vector_id_to_chunk_id.pop(vector_id, None)
        self._rebuild_index()
        self._save()

    def clear(self):
        self.index = faiss.IndexFlatIP(1024)
        self.chunk_id_to_vector_id = {}
        self.vector_id_to_chunk_id = {}
        self._save()

    def _rebuild_index(self):
        """Rebuild the flat index while preserving every mapped vector."""
        dimension = int(self.index.d) if self.index is not None else 1024
        old_index = self.index
        kept = sorted(
            (int(vector_id), chunk_id)
            for chunk_id, vector_id in self.chunk_id_to_vector_id.items()
            if old_index is not None and 0 <= int(vector_id) < old_index.ntotal
        )
        new_index = faiss.IndexFlatIP(dimension)
        if kept:
            vectors = np.vstack([
                np.asarray(old_index.reconstruct(vector_id), dtype=np.float32)
                for vector_id, _ in kept
            ])
            new_index.add(vectors)

        self.index = new_index
        self.chunk_id_to_vector_id = {
            chunk_id: new_id for new_id, (_, chunk_id) in enumerate(kept)
        }
        self.vector_id_to_chunk_id = {
            new_id: chunk_id for chunk_id, new_id in self.chunk_id_to_vector_id.items()
        }

    def _save(self):
        faiss.write_index(self.index, str(self.index_path))
        with open(self.metadata_path, "wb") as f:
            pickle.dump({
                "chunk_to_vec": self.chunk_id_to_vector_id,
            }, f)
        # Notify sibling instances on the same directory to reload state
        for store in list(_all_instances):
            if store is not self and store.index_dir == self.index_dir:
                store._load_or_create()

    def reload(self):
        """Reload state from disk (call after external writes)."""
        self._load_or_create()


def get_store(index_dir: Path) -> VectorStore:
    """Factory: returns a cached VectorStore keyed by resolved index_dir."""
    key = Path(index_dir).resolve()
    if key not in _store_cache:
        _store_cache[key] = VectorStore(key)
    return _store_cache[key]
