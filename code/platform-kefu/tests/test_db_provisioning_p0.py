"""
P0 regression: database provisioning & tenant schema contract.

Assertions are static (text/pathlib) — no MySQL connection, no pytest.
Use:  python -m unittest tests.test_db_provisioning_p0
      from ``code\\platform-kefu``.

Context (2026-08 / TDD RED):
  - docker/mysql/init.sql creates only platform / platform_message / platform_nacos
    but is MISSING platform_kefu.
  - docker-compose.yml platform-kefu service expects MYSQL_DATABASE=platform_kefu
    and the Kefu service will connect to a non-existent db on first boot.
  - CREATE_TABLES_SQL in app/models/database.py defines kefu_session / kefu_message
    without a tenant_id column (tenant-scoped isolation is absent).

Existing installations must apply a migration + backfill after these tests turn GREEN:
  1. Add ``CREATE DATABASE IF NOT EXISTS platform_kefu ...`` + GRANT to init.sql
  2. ALTER TABLE kefu_session / kefu_message ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0
     (platform tenant quarantine; backfill legacy rows before enabling non-zero tenants).
  3. Update any existing INSERT / SELECT to carry tenant_id.
"""
from __future__ import annotations

import re
import unittest
from pathlib import Path

# ---------------------------------------------------------------------------
# Paths — anchor to repo root so the test is runnable from any CWD
# ---------------------------------------------------------------------------
_REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
_INIT_SQL = _REPO_ROOT / "docker" / "mysql" / "init.sql"
_DB_SCHEMA_PY = (
    _REPO_ROOT / "code" / "platform-kefu" / "app" / "models" / "database.py"
)
_COMPOSE = _REPO_ROOT / "docker-compose.yml"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


# ---------------------------------------------------------------------------
# Helpers for SQL text assertions
# ---------------------------------------------------------------------------
# Normalize whitespace so newlines / indentation don't matter.
_WS_RE = re.compile(r"\s+")


def _norm(s: str) -> str:
    return _WS_RE.sub(" ", s).lower()


# Match "CREATE DATABASE IF NOT EXISTS <name>" — captures the db name.
_RE_CREATE_DB = re.compile(
    r"create\s+database\s+if\s+not\s+exists\s+(\w+)", re.IGNORECASE
)

# Match "GRANT <privs> ON <db>.* TO <user>@<host>" — captures the db name.
_RE_GRANT = re.compile(
    r"grant\s+[\w\s]+?\s+on\s+(\S+)\.\*\s+to\s+", re.IGNORECASE
)

# Collect identifiers inside backticks or bare words.
_RE_DB_IDENTIFIER = re.compile(r"`?(\w+)`?", re.IGNORECASE)


def _extract_dbs(text: str) -> set[str]:
    """Return all database names referenced by CREATE DATABASE or GRANT."""
    dbs: set[str] = set()
    for m in _RE_CREATE_DB.finditer(text):
        dbs.add(m.group(1).lower())
    for m in _RE_GRANT.finditer(text):
        # "platform_kefu.*" → "platform_kefu"
        dbpart = m.group(1)
        raw = _RE_DB_IDENTIFIER.findall(dbpart)
        if raw:
            dbs.add(raw[0].lower())
    return dbs


def _find_block(sql: str, table: str) -> str | None:
    """Find the CREATE TABLE block for *table* (case-insensitive).

    Returns the raw block text or None.
    """
    pattern = re.compile(
        rf"(?:CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?)\s*`?{re.escape(table)}`?\s*\(",
        re.IGNORECASE,
    )
    m = pattern.search(sql)
    if not m:
        return None
    # Find the closing ");" — simple greedy walk; sufficient for single-table blocks.
    start = m.start()
    depth = 0
    i = start
    while i < len(sql):
        ch = sql[i]
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                # Consume trailing ";".
                j = i + 1
                while j < len(sql) and sql[j] in (" ", "\t", "\n", "\r"):
                    j += 1
                if sql[j : j + 1] == ";":
                    j += 1
                return sql[start:j]
        i += 1
    return sql[start:]


def _cols_of(block: str) -> set[str]:
    """Extract column names from a CREATE TABLE block (naive, case-insensitive)."""
    cols: set[str] = set()
    # Remove the leading "CREATE TABLE ... (" wrapper.
    inner = re.sub(r"^CREATE\s+TABLE.+?\(", "", block, flags=re.IGNORECASE)
    inner = re.sub(r"\)\s*ENGINE.*$", "", inner, flags=re.IGNORECASE)
    for line in inner.split("\n"):
        line = line.strip().rstrip(",")
        if not line:
            continue
        # First token is the column name (skip KEY/INDEX/PRIMARY/FULLTEXT prefixes).
        tokens = line.split()
        if not tokens:
            continue
        name = tokens[0].strip("`")
        if name.upper() in (
            "INDEX",
            "KEY",
            "PRIMARY",
            "UNIQUE",
            "FULLTEXT",
            "CONSTRAINT",
        ):
            continue
        cols.add(name.lower())
    return cols


def _indexes_of(block: str) -> set[str]:
    """Extract indexed column references from a CREATE TABLE block."""
    idxs: set[str] = set()
    # Match INDEX/KEY/UNIQUE KEY / FULLTEXT INDEX declarations.
    for m in re.finditer(
        r"(?:INDEX|KEY|UNIQUE\s+KEY|FULLTEXT\s+INDEX)\s+"
        r"(?:\w+\s+)?\(([^)]+)\)",
        block,
        re.IGNORECASE,
    ):
        for col in m.group(1).split(","):
            col = col.strip().strip("`")
            if col:
                idxs.add(col.lower())
    return idxs


def _load_schema_sql() -> str:
    """Extract CREATE_TABLES_SQL body from database.py (module-level helper)."""
    text = _read(_DB_SCHEMA_PY)
    # Triple-quoted string body — match the full multiline constant.
    m = re.search(r"CREATE_TABLES_SQL\s*=\s*\"\"\"(.+?)\"\"\"", text, re.DOTALL)
    if not m:
        m = re.search(r"CREATE_TABLES_SQL\s*=\s*'''(.+?)'''", text, re.DOTALL)
    if not m:
        # Fallback: grab between the first """ and the next """.
        idx1 = text.find('"""')
        idx2 = text.find('"""', idx1 + 3)
        if idx1 == -1 or idx2 == -1:
            raise AssertionError(
                "Cannot find CREATE_TABLES_SQL constant in database.py"
            )
        return text[idx1 + 3 : idx2]
    return m.group(1)


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------
class TestInitSqlProvisionsPlatformKefu(unittest.TestCase):
    """init.sql must create + grant platform_kefu."""

    def test_init_sql_exists(self):
        self.assertTrue(_INIT_SQL.exists(), f"missing {_INIT_SQL}")

    def test_creates_platform_kefu_database(self):
        text = _read(_INIT_SQL)
        dbs = _extract_dbs(text)
        self.assertIn(
            "platform_kefu",
            dbs,
            f"init.sql does not CREATE DATABASE platform_kefu. Found dbs={dbs}",
        )

    def test_grants_platform_user_on_platform_kefu(self):
        text = _read(_INIT_SQL)
        dbs = _extract_dbs(text)
        self.assertIn(
            "platform_kefu",
            dbs,
            "init.sql does not GRANT platform user privileges on platform_kefu.*",
        )


class TestComposeTargetsPlatformKefu(unittest.TestCase):
    """docker-compose.yml should already connect kefu service to platform_kefu.

    If this fails, the compose file is out of sync with the intended schema.
    """

    def test_kefu_service_has_mysql_database_platform_kefu(self):
        text = _read(_COMPOSE)
        self.assertIn(
            "platform_kefu",
            text,
            "docker-compose.yml missing MYSQL_DATABASE=platform_kefu for platform-kefu service",
        )

    def test_internal_token_is_required_in_both_services(self):
        text = _read(_COMPOSE)
        self.assertNotIn("KEFU_INTERNAL_TOKEN=${KEFU_INTERNAL_TOKEN:-}", text)
        self.assertEqual(
            text.count("KEFU_INTERNAL_TOKEN=${KEFU_INTERNAL_TOKEN:?"),
            2,
            "gateway and kefu must both fail fast when KEFU_INTERNAL_TOKEN is unset",
        )


class TestKefuSchemaContract(unittest.TestCase):
    """Tenant-scoped contract for the Kefu tables.

    Required fields (per DESIGN § tenant isolation):
      - kefu_session: tenant_id (BIGINT, NOT NULL)
      - kefu_message: tenant_id (BIGINT, NOT NULL)

    Required indexes:
      - idx_session_tenant on kefu_session(customer_id, tenant_id) at minimum,
        OR a composite (tenant_id, customer_id) if preferred — assert at least
        tenant_id presence.
      - idx_message_tenant on kefu_message(session_id, tenant_id) or tenant_id alone.
    """

    def _load_schema_sql(self) -> str:
        text = _read(_DB_SCHEMA_PY)
        # Extract the CREATE_TABLES_SQL triple-quoted string body.
        m = re.search(
            r'CREATE_TABLES_SQL\s*=\s*"""(.+?)"""', text, re.DOTALL
        )
        self.assertIsNotNone(
            m,
            "Cannot find CREATE_TABLES_SQL constant in database.py",
        )
        return m.group(1)

    def test_kefu_session_has_tenant_id_column(self):
        sql = _load_schema_sql()
        block = _find_block(sql, "kefu_session")
        self.assertIsNotNone(block, "CREATE TABLE kefu_session not found in database.py")
        cols = _cols_of(block)
        self.assertIn(
            "tenant_id",
            cols,
            "kefu_session missing tenant_id column — tenant isolation not enforced",
        )

    def test_kefu_message_has_tenant_id_column(self):
        sql = _load_schema_sql()
        block = _find_block(sql, "kefu_message")
        self.assertIsNotNone(block, "CREATE TABLE kefu_message not found in database.py")
        cols = _cols_of(block)
        self.assertIn(
            "tenant_id",
            cols,
            "kefu_message missing tenant_id column — tenant isolation not enforced",
        )

    def test_kefu_session_has_tenant_index(self):
        sql = _load_schema_sql()
        block = _find_block(sql, "kefu_session")
        self.assertIsNotNone(block)
        idxs = _indexes_of(block)
        self.assertIn(
            "tenant_id",
            idxs,
            "kefu_session missing an index that covers tenant_id — "
            "tenant-scoped lookups will be full-table scans",
        )

    def test_kefu_message_has_tenant_index(self):
        sql = _load_schema_sql()
        block = _find_block(sql, "kefu_message")
        self.assertIsNotNone(block)
        idxs = _indexes_of(block)
        self.assertIn(
            "tenant_id",
            idxs,
            "kefu_message missing an index that covers tenant_id",
        )

    def test_existing_tables_still_present(self):
        """Regression guard: adding tenant_id must not drop documents/chunks/ask_logs."""
        sql = _load_schema_sql()
        for tbl in ("documents", "chunks", "ask_logs", "kefu_faq", "kefu_data_source"):
            block = _find_block(sql, tbl)
            self.assertIsNotNone(
                block,
                f"Expected table {tbl} still present in CREATE_TABLES_SQL",
            )


if __name__ == "__main__":
    unittest.main()
