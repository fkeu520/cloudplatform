"""
P0 regression: pooled MySQL connections must be RETURNED to the pool, not closed.

Assertions
----------
1. Behavioural (``TestPooledConnection``): ``get_db_connection()`` hands back an
   object whose ``close()`` calls ``Pool.release(conn)`` and does **not** call
   ``Connection.close()``; ``close()`` is idempotent; attribute access is
   forwarded to the real connection. Driven by a fake pool/connection, so no
   MySQL is required.
2. Contract (``TestAcquireContract``): acquiring is bounded by
   ``settings.mysql_acquire_timeout`` and raises ``PoolExhaustedError`` instead
   of blocking forever.
3. Static (``TestNoLeakyCallSites``): every ``await get_db_connection()`` in the
   service is followed by a ``close()`` on a ``finally`` path, and no module
   reaches for ``aiomysql.Pool.acquire()`` directly.

Context (RED, 2026-09-30, host 142)
-----------------------------------
The service died after five requests. ``get_db_connection()`` returned
``await pool.acquire()`` — a *pooled* connection — and every call site released it
with ``conn.close()``. In aiomysql 0.3.2 ``Connection.close()`` only tears down
the socket; the connection stays in ``Pool._used`` until ``Pool.release()`` runs.
Measured on the live container:

    after create : free=1 size=1 used=0
    iter 1 ok    : free=0 size=1 used=1
    ...
    iter 5 ok    : free=0 size=5 used=5
    iter 6       : *** pool.acquire() TIMED OUT -> pool exhausted ***

So the sixth DB-touching request blocked on the pool's condition variable
permanently, while ``/api/kefu/health`` (no DB access) kept answering 200 and the
container looked healthy. The browser flow is exactly four requests
(``POST /sessions`` → ``GET /sessions/my`` → ``GET /sessions/{sid}`` →
``GET /messages``), which made "sending the message" the first thing to hang.

Use:  python -m unittest tests.test_db_pool_release_p0
      from ``code\\platform-kefu``.
"""
from __future__ import annotations

import asyncio
import re
import sys
import types
import unittest
from pathlib import Path

# ---------------------------------------------------------------------------
# app.models.database imports aiomysql, which is a server-side dependency and is
# not installed in every checkout that runs these tests (the sibling P0 suites
# stay import-free for the same reason). Stub it before the import so the
# behavioural assertions below still run anywhere. database.py only touches
# ``aiomysql.create_pool`` at runtime and ``aiomysql.Pool`` /
# ``aiomysql.Connection`` in annotations evaluated at module level, so the stub
# needs exactly those three names.
# ---------------------------------------------------------------------------
if "aiomysql" not in sys.modules:
    try:  # pragma: no cover - exercised only where the real dep is present
        import aiomysql  # noqa: F401
    except ModuleNotFoundError:
        _stub = types.ModuleType("aiomysql")

        class _Pool:  # noqa: D401 - annotation placeholder only
            ...

        class _Connection:  # noqa: D401 - annotation placeholder only
            ...

        _stub.Pool = _Pool
        _stub.Connection = _Connection

        def _unavailable(*_args, **_kwargs):
            raise RuntimeError("aiomysql is not installed in this environment")

        _stub.create_pool = _unavailable
        sys.modules["aiomysql"] = _stub

from app.models import database as db

_APP_DIR = Path(__file__).resolve().parent.parent / "app"


# ---------------------------------------------------------------------------
# Fakes — a real aiomysql.Pool needs a live MySQL, and the whole point of the
# fix is *which* pool method gets called, so a hand-rolled double is both
# sufficient and the most precise assertion surface.
# ---------------------------------------------------------------------------
class _FakeConn:
    """Mimics aiomysql.Connection: ``close()`` tears down the socket and tells the
    pool nothing, which is precisely the behaviour under test."""

    def __init__(self, name: str = "conn") -> None:
        self.name = name
        self.closed = False
        self.commits = 0

    def close(self) -> None:
        self.closed = True

    def cursor(self, *args, **kwargs):
        return "cursor-context-manager"

    async def commit(self) -> None:
        self.commits += 1


class _FakePool:
    """Mimics the parts of aiomysql.Pool that matter: acquire() moves a
    connection into ``_used``, and release() asserts it is there before handing
    it back. A real Pool needs a live MySQL; which of these two methods gets
    called is exactly the regression under test."""

    def __init__(self) -> None:
        self.released: list[_FakeConn] = []
        self.used: set[_FakeConn] = set()
        self._free: list[_FakeConn] = []

    def acquire(self):
        async def _acquire():
            if not self._free:
                await asyncio.Event().wait()  # never resolves, like the real pool
            conn = self._free.pop()
            self.used.add(conn)
            return conn

        return _acquire()

    def release(self, conn: _FakeConn) -> None:
        assert conn in self.used, "released a connection the pool never handed out"
        self.used.remove(conn)
        self.released.append(conn)
        self._free.append(conn)

    def checkout(self) -> _FakeConn:
        """Hand out a connection the way production does, so ``_used`` is
        populated before the proxy tries to release it."""
        conn = _FakeConn(f"conn-{len(self._free)}")
        self._free.append(conn)
        self.used.add(conn)
        self._free.remove(conn)
        return conn


class TestPooledConnection(unittest.TestCase):
    def test_close_releases_to_pool_instead_of_closing_socket(self) -> None:
        pool = _FakePool()
        conn = pool.checkout()
        pooled = db._PooledConnection(pool, conn)

        pooled.close()

        self.assertEqual(pool.released, [conn], "close() must return the connection")
        self.assertFalse(
            conn.closed,
            "close() must NOT tear down the socket — a released connection gets reused",
        )

    def test_close_is_idempotent(self) -> None:
        """docs.py error paths can reach close() twice; a second release would
        trip aiomysql's ``assert conn in self._used``."""
        pool = _FakePool()
        pooled = db._PooledConnection(pool, pool.checkout())

        pooled.close()
        pooled.close()
        pooled.close()

        self.assertEqual(len(pool.released), 1)

    def test_attributes_are_forwarded_to_the_real_connection(self) -> None:
        """The 36 existing call sites use conn.cursor() and conn.commit() —
        they must keep working untouched."""
        pool = _FakePool()
        conn = pool.checkout()
        pooled = db._PooledConnection(pool, conn)

        self.assertEqual(pooled.cursor(), "cursor-context-manager")
        self.assertEqual(pooled.name, conn.name)

        asyncio.run(pooled.commit())
        self.assertEqual(conn.commits, 1)

    def test_supports_async_with(self) -> None:
        pool = _FakePool()
        conn = pool.checkout()
        pooled = db._PooledConnection(pool, conn)

        async def _use():
            async with pooled as raw:
                self.assertIs(raw, conn)

        asyncio.run(_use())
        self.assertEqual(pool.released, [conn])


class TestAcquireContract(unittest.TestCase):
    def test_acquire_timeout_raises_instead_of_hanging(self) -> None:
        """An exhausted pool must surface as PoolExhaustedError (503 in main.py),
        not as a request that never returns."""
        pool = _FakePool()  # empty _free -> acquire() blocks forever
        original_pool, original_timeout = db.POOL, db.settings.mysql_acquire_timeout
        db.POOL = pool
        db.settings.mysql_acquire_timeout = 0.05
        try:
            with self.assertRaises(db.PoolExhaustedError):
                asyncio.run(db.get_db_connection())
        finally:
            db.POOL = original_pool
            db.settings.mysql_acquire_timeout = original_timeout

    def test_sequential_requests_never_exhaust_the_pool(self) -> None:
        """The regression, end to end: eight back-to-back requests through
        get_db_connection() must all come from the one pooled connection.

        Pre-fix this wedges on request two — get_db_connection() handed back a
        pooled connection that the call site closed instead of releasing, so the
        fake pool's ``_free`` list drained and ``acquire()`` blocked forever.
        The outer wait_for keeps that failure bounded instead of hanging CI.
        """
        pool = _FakePool()
        conn = pool.checkout()
        pool._free.append(conn)  # the single connection the pool may hand out
        original_pool, original_timeout = db.POOL, db.settings.mysql_acquire_timeout
        db.POOL = pool
        db.settings.mysql_acquire_timeout = 0.5

        async def _eight_requests():
            for _ in range(8):
                pooled = await db.get_db_connection()
                pooled.close()

        try:
            asyncio.run(asyncio.wait_for(_eight_requests(), timeout=5))
        except asyncio.TimeoutError:
            self.fail(
                "get_db_connection() wedged after the first request — the "
                "connection is not being returned to the pool"
            )
        finally:
            db.POOL = original_pool
            db.settings.mysql_acquire_timeout = original_timeout

        self.assertEqual(
            len(pool.released), 8, "every request must release its connection"
        )
        self.assertFalse(conn.closed, "the pooled socket must survive the round trip")


class TestNoLeakyCallSites(unittest.TestCase):
    """Static guard: the regression above came from a whole-service pattern, so
    a new bare ``pool.acquire()`` must not sneak back in."""

    _GET_CONN = re.compile(r"conn = await get_db_connection\(\)")

    def test_no_module_bypasses_the_pool_helper(self) -> None:
        offenders = [
            p.relative_to(_APP_DIR).as_posix()
            for p in _APP_DIR.rglob("*.py")
            if re.search(r"(?<!def )\bpool\.acquire\(\)", p.read_text(encoding="utf-8"))
            and p.name != "database.py"
        ]
        self.assertEqual(
            offenders, [], f"these modules acquire raw pool connections: {offenders}"
        )

    def test_every_acquire_has_a_close(self) -> None:
        for path in sorted(_APP_DIR.rglob("*.py")):
            if path.name == "database.py":
                continue  # defines the contract; also mentions the pattern in prose
            text = path.read_text(encoding="utf-8")
            acquires = len(TestNoLeakyCallSites._GET_CONN.findall(text))
            if not acquires:
                continue
            with self.subTest(module=path.relative_to(_APP_DIR).as_posix()):
                # closes >= acquires, not ==: mutually-exclusive early-return
                # paths legitimately add a second close on the same connection
                # (docs.py delete/reprocess 404 branches).
                self.assertGreaterEqual(
                    text.count("conn.close()"),
                    acquires,
                    "every get_db_connection() must be paired with conn.close()",
                )


if __name__ == "__main__":
    unittest.main(verbosity=2)
