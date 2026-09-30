"""文档切片 — 按字符切分, CJK 感知。

2026-09-30 修复 P0: 真实文档上传必然 500
------------------------------------------------
旧实现按 ``text.split()`` (即空格分词) 计算长度:

    words = para.split()
    if len(current.split()) + len(words) <= self.chunk_size:
        ...
    else:
        current = para          # 单段超限时只是整段塞进去, 从不切开

中文没有空格, ``"一整篇文档".split()`` 恒为 ``["一整篇文档"]`` (长度 1),
所以:

1. ``512`` 这个上限**永远不会被触发**, 切分退化成"整篇一整块";
2. ``estimate_tokens()`` 同样返回 ``len(text.split())``, 中文恒为 1 ——
   450KB 文档在 chunks 表里被记成 "1 个 token", 统计完全失真;
3. 整块文本送进 SiliconFlow embeddings, 超过 BAAI/bge-m3 的 8192 token
   单条上限, 接口返回 400 ``code 20015 参数无效``;
4. ``SiliconFlowEmbedder.embed()`` 直接 ``raise_for_status()``, upload 抛
   HTTPException 500, 前端只显示笼统的"上传失败", 且 kefu 日志只有一行
   httpx 的 URL, 极难定位。

142 实测阈值 (BAAI/bge-m3):
    单条 8003 tokens      -> OK
    单条 42000 字符       -> FAIL
    英文 500 词(6503 tok) -> OK / 英文 2000 词 -> FAIL
    批量 128 条 x 1001 字符 -> OK   (批量条数不是瓶颈, 单条长度才是)

因此改为**按字符硬切**, 并对超长段落做二次切分, 保证任何单块都远低于
8192 token。默认 1000 字符: 中文约 1000 token, 英文约 250 token。
"""
from __future__ import annotations

import re

# 句子/子句边界: 中英文句末标点 + 分号 + 换行
_SENT_BOUNDARY = re.compile(r"(?<=[。！？!?；;\n])")

# 逐字符计 1 token 的区间 (CJK 汉字 + 日文假名 + 全角标点)
_WIDE = re.compile(r"[\u1100-\u115F\u2E80-\uA4CF\uAC00-\uD7A3"
                   r"\uF900-\uFAFF\uFE30-\uFE4F\uFF00-\uFF60\uFFE0-\uFFE6]")


class Chunker:
    """按字符切片。``chunk_size``/``overlap`` 单位均为**字符**。"""

    # 1000 字符 ≈ 中文 1000 token, 距 8192 上限留了 8 倍余量;
    # 实测 128 条 x 1001 字符批量调用正常。
    DEFAULT_CHUNK_CHARS = 1000
    DEFAULT_OVERLAP_CHARS = 100

    def __init__(
        self,
        chunk_size: int = DEFAULT_CHUNK_CHARS,
        overlap: int = DEFAULT_OVERLAP_CHARS,
    ):
        self.chunk_size = max(1, int(chunk_size))
        # overlap 必须小于 chunk_size, 否则会原地打转
        self.overlap = max(0, min(int(overlap), self.chunk_size - 1))
        # 打包预算: 补重叠后总长仍须 <= chunk_size, 所以先按此预算打包
        self._pack_budget = self.chunk_size - self.overlap

    # ------------------------------------------------------------------ #
    def split(self, text: str) -> list[str]:
        """把 ``text`` 切成不超过 ``chunk_size`` 字符的块列表。"""
        if not text or not text.strip():
            return []

        units: list[str] = []
        for para in re.split(r"\n\s*\n", text.strip()):
            para = para.strip()
            if not para:
                continue
            units.extend(self._split_long(para))

        if not units:
            return []

        # 贪心打包: 尽量把多个小段合并到一个块里
        chunks: list[str] = []
        current = ""
        for unit in units:
            if not current:
                current = unit
            elif len(current) + len(unit) <= self._pack_budget:
                current = current + "\n\n" + unit
            else:
                chunks.append(current)
                current = unit
        if current:
            chunks.append(current)

        return self._apply_overlap(chunks)

    def _split_long(self, para: str) -> list[str]:
        """把超过打包预算的段落切开: 先按句子, 再按硬字符上限。

        硬上限用 ``_pack_budget``(= chunk_size - overlap) 而不是 chunk_size:
        补重叠时会在块首再拼 overlap 个字符, 若这里按 chunk_size 切,
        补完就会变成 chunk_size + overlap, 契约就破了。
        """
        if len(para) <= self._pack_budget:
            return [para]

        pieces: list[str] = []
        buf = ""
        for sent in _SENT_BOUNDARY.split(para):
            if not sent:
                continue
            # 单句本身就超长 (例如没有标点的长串) -> 按预算硬切
            while len(sent) > self._pack_budget:
                if buf:
                    pieces.append(buf)
                    buf = ""
                pieces.append(sent[: self._pack_budget])
                sent = sent[self._pack_budget:]
            if len(buf) + len(sent) <= self._pack_budget:
                buf += sent
            else:
                if buf:
                    pieces.append(buf)
                buf = sent
        if buf:
            pieces.append(buf)
        return [p for p in pieces if p.strip()]

    def _apply_overlap(self, chunks: list[str]) -> list[str]:
        """相邻块之间补 ``overlap`` 字符的重叠, 避免语义在边界被切断。

        重叠是在 ``_pack_budget = chunk_size - overlap`` 的预算内预留的,
        所以补完之后每块长度仍然 <= chunk_size。
        """
        if self.overlap <= 0 or len(chunks) < 2:
            return chunks
        merged: list[str] = []
        for i, chunk in enumerate(chunks):
            if i == 0:
                merged.append(chunk)
                continue
            tail = chunks[i - 1][-self.overlap:]
            merged.append(tail + chunk)
        return merged

    def estimate_tokens(self, text: str) -> int:
        """估算 token 数。

        旧实现是 ``len(text.split())`` —— 中文恒为 1, 导致 chunks.token_count
        完全失真。改为: 宽字符(中日韩全角)按 1 计, 其余按 4 字符 ≈ 1 token 计。
        """
        if not text:
            return 0
        wide = len(_WIDE.findall(text))
        narrow = len(text) - wide
        return wide + (narrow + 3) // 4
