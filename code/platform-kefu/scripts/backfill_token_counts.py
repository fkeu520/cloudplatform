"""一次性的 chunks.token_count 脏数据回填脚本。

背景
----
旧 ``Chunker.estimate_tokens`` 用的是 ``len(text.split())``, 中文无空格
的文本会被恒记为 ``1`` 个 token。修复后的实现是"宽字符(中日韩全角)按 1 计,
其余按 4 字符 ≈ 1 token 计"。代码已修, 但历史行未回填。

运行方式 (容器内)
-----------------
::

    docker exec platform-kefu python scripts/backfill_token_counts.py
    docker exec platform-kefu python scripts/backfill_token_counts.py --dry-run
    docker exec platform-kefu python scripts/backfill_token_counts.py --batch-size 200

幂等: 已修正的行会再次被重算, 但 ``new == old`` 不会触发 UPDATE; 二跑扫描
全部完成后 changed=0。

关键决策
--------
不采用启发式过滤 (例如 ``token_count < LENGTH(content)*0.05``):
MySQL ``LENGTH()`` 对 utf8mb4 按**字节**计 —— 2 个汉字在 db 里是 6 字节,
阈 0.3 → 漏判。改为**全量重算**, 用主键游标分页, 默认每批 500 条。

设计约束
--------
* 复用 ``app.models.database.get_db_connection()`` + ``conn.close()`` 的
  try/finally 模式, 不破坏现有 36 处调用点。
* diff 逻辑必须是纯函数 ``compute_token_fixes(rows)``, 可在无 DB 环境下
  被 pytest 验证。
"""
from __future__ import annotations

import argparse
import asyncio
import sys
import time
from typing import Iterable, Sequence, Tuple

from app.services.chunker import Chunker


# --------------------------------------------------------------------------
# 纯函数: 可被单元测试直接调用, 无需 DB / 环境变量
# --------------------------------------------------------------------------

def compute_token_fixes(
    rows: Iterable[Tuple[str, str, int]],
) -> list[Tuple[str, int]]:
    """对一批 ``chunk_id, content, old_token_count`` 重算 token_count,
    只返回**需要更新**的 ``(chunk_id, new_token_count)`` 列表。

    参数
    ----
    rows : iterable of (chunk_id: str, content: str, old_token_count: int)
        典型来源: ``SELECT chunk_id, content, token_count FROM chunks``.

    返回
    ----
    list of (chunk_id: str, new_token_count: int), 仅含 new != old 的行。
    """
    c = Chunker()
    fixes: list[Tuple[str, int]] = []
    for chunk_id, content, old_token_count in rows:
        new_token_count = c.estimate_tokens(content)
        if new_token_count != old_token_count:
            fixes.append((chunk_id, new_token_count))
    return fixes


# --------------------------------------------------------------------------
# CLI 入口
# --------------------------------------------------------------------------

async def _run(args: argparse.Namespace) -> int:
    """主异步逻辑: 分批读取 chunks 表, 统计并写回差异。"""
    from app.models.database import get_db_connection  # 推迟 import 避免启动期强制连库

    conn = None
    scanned = 0
    changed = 0
    unchanged = 0
    errors: list[str] = []

    start = time.monotonic()
    try:
        conn = await get_db_connection()
        last_chunk_id: str | None = None

        while True:
            # 主键游标分页: 不依赖 OFFSET (大表慢, 且有 hole 风险)
            if last_chunk_id is None:
                where = ""
                params: object = ()
            else:
                where = " AND chunk_id > %s"
                params = (last_chunk_id,)

            # aiomysql: execute/fetchall 在 cursor 上, Connection 没有这两个方法
            async with conn.cursor() as cur:
                await cur.execute(
                    f"SELECT chunk_id, content, token_count FROM chunks"
                    f" WHERE 1=1{where}"
                    f" ORDER BY chunk_id ASC"
                    f" LIMIT %s",
                    (*params, args.batch_size),
                )
                rows = await cur.fetchall()
            if not rows:
                break

            for row in rows:
                scanned += 1
                last_chunk_id = row[0]  # chunk_id 是 VARCHAR(64) PK

            fixes = compute_token_fixes(rows)
            if args.dry_run:
                changed += len(fixes)
                unchanged += len(rows) - len(fixes)
                continue

            if fixes:
                # 逐个参数化 UPDATE, 避免构造 IN(...) 超长
                async with conn.cursor() as cur:
                    for chunk_id, new_count in fixes:
                        try:
                            await cur.execute(
                                "UPDATE chunks SET token_count=%s WHERE chunk_id=%s",
                                (new_count, chunk_id),
                            )
                            changed += 1
                        except Exception as exc:  # 单行失败不中断整体
                            errors.append(f"UPDATE {chunk_id}: {exc}")
                unchanged += len(rows) - len(fixes)
            else:
                unchanged += len(rows)

    except Exception as exc:  # noqa: BLE001
        errors.append(f"unexpected error: {exc}")
        return 2
    finally:
        if conn is not None:
            conn.close()

    elapsed = time.monotonic() - start
    print(f"scanned={scanned} changed={changed} unchanged={unchanged} elapsed={elapsed:.2f}s")
    if errors:
        for e in errors:
            print(f"  ERR: {e}", file=sys.stderr)
        return 1
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(
        description="回填 chunks.token_count 脏数据 (一次性, 幂等)"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="只统计将变更行数, 不写库",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=500,
        help="每批扫描行数 (默认 500)",
    )
    args = parser.parse_args()
    rc = asyncio.run(_run(args))
    sys.exit(rc)


if __name__ == "__main__":
    main()
