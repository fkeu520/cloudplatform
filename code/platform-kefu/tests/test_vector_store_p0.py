"""
Regression tests for confirmed FAISS P0 bugs in VectorStore.

P0 Bug #1: delete_by_chunk_ids destroys all vectors — _rebuild_index only creates a
           fresh empty index and never copies remaining vectors back. Asserting that
           after deleting ONE chunk the remaining chunks are still searchable.

P0 Bug #2: Module-level stores have independent chunk mappings — a shared-store
           factory (or two instances pointing at the same index_dir) must return
           consistent state: writes through one instance are visible to another.

Run with:  python -m unittest code.platform_kefu.tests.test_vector_store_p0 -v
"""

import os
import pickle
import shutil
import tempfile
import unittest
from pathlib import Path

import faiss
import numpy as np

# Import directly from the service module (no pytest, no fixtures)
# path is relative to repo root when run via `python -m unittest code.platform_kefu.tests.test_vector_store_p0`
import sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "code", "platform-kefu"))
from app.services.vector_store import VectorStore


_VECTOR_COUNTER = 0


def _rand_norm_vec(dim: int = 1024) -> np.ndarray:
    """Return a deterministic orthogonal unit vector for stable search tests."""
    global _VECTOR_COUNTER
    v = np.zeros(dim, dtype=np.float32)
    v[_VECTOR_COUNTER % dim] = 1.0
    _VECTOR_COUNTER += 1
    return v


class TestDeletePreservesOtherVectors(unittest.TestCase):
    """P0 Bug #1: deleting one chunk must NOT wipe out other vectors."""

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="vs_p0_delete_"))
        self.store = VectorStore(self.tmp)
        # Seed with 5 distinct chunks
        self.chunk_ids = [f"chunk-{i}" for i in range(5)]
        self.vectors = [_rand_norm_vec(1024) for _ in range(5)]
        self.vids = self.store.add_vectors(self.chunk_ids, self.vectors)
        self.assertEqual(len(self.vids), 5)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def _search_all(self, store, k=5):
        """Return list of (chunk_id, score) for each seeded vector."""
        results = []
        for vid, vec in zip(self.vids, self.vectors):
            hits = store.search(vec, top_k=k)
            cids = {cid for cid, _ in hits}
            results.append(cids)
        return results

    def test_delete_one_preserves_four(self):
        """After removing chunk-2, all other 4 chunks remain searchable."""
        deleted = ["chunk-2"]
        self.store.delete_by_chunk_ids(deleted)

        # Verify chunk-2 is gone
        self.assertNotIn("chunk-2", self.store.chunk_id_to_vector_id)
        self.assertNotIn("chunk-2", set(self.store.vector_id_to_chunk_id.values()))

        # Verify the other 4 are still present in mappings
        remaining_chunks = {f"chunk-{i}" for i in range(5) if i != 2}
        self.assertEqual(set(self.store.chunk_id_to_vector_id.keys()), remaining_chunks)

        # Verify each remaining vector still returns itself in search
        for cid in remaining_chunks:
            vec = dict(zip(self.chunk_ids, self.vectors))[cid]
            hits = self.store.search(vec, top_k=2)
            hit_cids = {c for c, _ in hits}
            self.assertIn(cid, hit_cids,
                          msg=f"Deleted chunk {cid} not found after deleting chunk-2")

    def test_delete_all_leaves_empty_index(self):
        """Deleting every chunk leaves an empty index (ntotal == 0)."""
        self.store.delete_by_chunk_ids(list(self.chunk_ids))
        self.assertEqual(self.store.index.ntotal, 0)
        self.assertEqual(self.store.search(_rand_norm_vec(1024)), [])
        self.assertEqual(self.store.chunk_id_to_vector_id, {})


class TestSingletonStoreSameDirectory(unittest.TestCase):
    """P0 Bug #2: Two VectorStore instances pointing at the same directory must
    share state — writes by one must be visible to the other."""

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="vs_p0_singleton_"))

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def test_writes_visible_across_instances(self):
        """Add via store_a; store_b (same dir) must see the new chunk."""
        store_a = VectorStore(self.tmp)
        store_b = VectorStore(self.tmp)

        chunk_ids = ["shared-1", "shared-2"]
        vectors = [_rand_norm_vec(1024), _rand_norm_vec(1024)]
        store_a.add_vectors(chunk_ids, vectors)

        # store_b must see both chunks in its mapping
        self.assertEqual(set(store_b.chunk_id_to_vector_id.keys()), set(chunk_ids))
        self.assertEqual(store_b.index.ntotal, 2)

        # Search through store_b must find what store_a wrote
        for cid, vec in zip(chunk_ids, vectors):
            hits = store_b.search(vec, top_k=2)
            hit_cids = {c for c, _ in hits}
            self.assertIn(cid, hit_cids,
                          msg=f"store_b cannot find chunk {cid} written by store_a")

    def test_deletes_visible_across_instances(self):
        """Delete via store_a; store_b must reflect the deletion."""
        store_a = VectorStore(self.tmp)
        store_b = VectorStore(self.tmp)

        chunk_ids = ["del-1", "del-2", "del-3"]
        vectors = [_rand_norm_vec(1024) for _ in range(3)]
        store_a.add_vectors(chunk_ids, vectors)

        store_a.delete_by_chunk_ids(["del-2"])

        # store_b must not see del-2
        self.assertNotIn("del-2", store_b.chunk_id_to_vector_id)
        self.assertEqual(store_b.index.ntotal, 2)

        # store_b must still find the surviving chunks
        remaining = ["del-1", "del-3"]
        for cid, vec in zip(remaining, vectors):
            hits = store_b.search(vec, top_k=2)
            hit_cids = {c for c, _ in hits}
            self.assertIn(cid, hit_cids)


class TestRebuildIndexDoesNotCorrupt(unittest.TestCase):
    """Additional regression: internal _rebuild_index must not silently drop vectors."""

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="vs_p0_rebuild_"))

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def test_rebuild_index_preserves_vectors(self):
        """Calling _rebuild_index directly (as delete does internally) must keep data."""
        store = VectorStore(self.tmp)
        chunk_ids = ["r-1", "r-2"]
        vectors = [_rand_norm_vec(1024), _rand_norm_vec(1024)]
        store.add_vectors(chunk_ids, vectors)

        # Simulate what delete_by_chunk_ids does: remove from mapping then rebuild
        deleted_vector_id = store.chunk_id_to_vector_id["r-1"]
        del store.chunk_id_to_vector_id["r-1"]
        del store.vector_id_to_chunk_id[deleted_vector_id]
        store._rebuild_index()
        store._save()

        # After rebuild, only r-2 should remain
        self.assertEqual(set(store.chunk_id_to_vector_id.keys()), {"r-2"})
        self.assertEqual(store.index.ntotal, 1)

        # The surviving vector must still be searchable
        hits = store.search(vectors[1], top_k=1)
        self.assertEqual(len(hits), 1)
        self.assertEqual(hits[0][0], "r-2")


if __name__ == "__main__":
    unittest.main()
