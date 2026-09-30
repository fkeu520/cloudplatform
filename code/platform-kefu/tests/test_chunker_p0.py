"""Chunker 回归测试 — 中文文档切片 (P0, 2026-09-30)

背景: 真实文档 (docx/pdf 抽出中文) 上传必然 500。根因是旧 Chunker 按
``text.split()`` (空格) 计数, 中文无空格 -> 整篇成为单块 -> 超过
BAAI/bge-m3 的 8192 token 单条上限 -> SiliconFlow 400 -> upload 500。

142 实测阈值:
    单条 8003 tokens OK / 42000 字符 FAIL
    英文 500 词 OK / 2000 词 FAIL
    批量 128 条 x 1001 字符 OK (条数不是瓶颈, 单条长度才是)

这些测试锁死"任何单块都不得超过 chunk_size"这个硬约束, 防止再次退化。
"""
import pytest

from app.services.chunker import Chunker


# 142 上真实失败的那个尺寸: 450KB 纯中文
PROD_REPRO_CHARS = 450_000
# bge-m3 单条上限 8192 token; 中文 1 字 ~= 1 token
BGE_M3_TOKEN_LIMIT = 8192


class TestChineseChunking:
    def test_long_chinese_text_is_split(self):
        """核心回归: 长中文必须被切成多块。"""
        text = "这是一段用于测试的中文文档内容。" * 2000  # ~26000 字
        chunks = Chunker().split(text)
        assert len(chunks) > 1, "长中文未被切分 —— 这是 500 的根因"

    def test_no_chunk_exceeds_chunk_size(self):
        """硬约束: 任何单块都不得超过 chunk_size。"""
        text = "中文内容测试。" * 100_000
        for size in (200, 512, 1000, 4000):
            chunker = Chunker(chunk_size=size)
            chunks = chunker.split(text)
            assert chunks, f"chunk_size={size} 切出空列表"
            for i, c in enumerate(chunks):
                assert len(c) <= size, (
                    f"chunk_size={size} 时第 {i} 块有 {len(c)} 字符, 超出上限"
                )

    def test_production_repro_450kb(self):
        """142 真实失败场景: 450KB 中文文档, 每块都必须在 token 上限内。"""
        text = "大段真实中文内容用于测试嵌入接口的批量与长度限制。" * 20_000
        assert len(text) >= PROD_REPRO_CHARS

        chunker = Chunker()
        chunks = chunker.split(text)
        assert len(chunks) > 1
        for c in chunks:
            tokens = chunker.estimate_tokens(c)
            assert tokens <= BGE_M3_TOKEN_LIMIT, (
                f"单块估算 {tokens} token, 超过 bge-m3 上限 {BGE_M3_TOKEN_LIMIT}"
            )

    def test_estimate_tokens_not_one_for_chinese(self):
        """旧实现 len(text.split()) 对中文恒为 1, 导致 token_count 失真。"""
        chunker = Chunker()
        text = "中文估算" * 100  # 400 字
        assert chunker.estimate_tokens(text) >= 400
        assert chunker.estimate_tokens("") == 0

    def test_estimate_tokens_ascii_denser_than_cjk(self):
        """同样长度, 中文应比英文消耗更多 token。"""
        chunker = Chunker()
        assert chunker.estimate_tokens("中" * 100) > chunker.estimate_tokens("a" * 100)


class TestEdgeCases:
    def test_empty_and_blank(self):
        chunker = Chunker()
        assert chunker.split("") == []
        assert chunker.split("   \n\n  ") == []

    def test_short_text_single_chunk(self):
        chunks = Chunker().split("很短的一段话。")
        assert len(chunks) == 1
        assert "很短的一段话" in chunks[0]

    def test_no_space_no_punctuation_giant_string(self):
        """无空格无标点的超长串也必须被硬切, 不能整块送出去。"""
        chunker = Chunker(chunk_size=500)
        chunks = chunker.split("啊" * 20_000)
        assert len(chunks) > 1
        for c in chunks:
            assert len(c) <= 500

    def test_english_still_chunks(self):
        text = "The quick brown fox jumps over the lazy dog. " * 5000
        chunker = Chunker(chunk_size=1000)
        chunks = chunker.split(text)
        assert len(chunks) > 1
        for c in chunks:
            assert len(c) <= 1000

    def test_paragraphs_preserved_when_small(self):
        a, b = "第一段内容。", "第二段内容。"
        chunks = Chunker(chunk_size=1000).split(a + "\n\n" + b)
        assert len(chunks) == 1
        assert a in chunks[0] and b in chunks[0]

    def test_content_preserved(self):
        """切片不能丢内容 (忽略分隔符差异)。"""
        text = "\n\n".join(f"第 {i} 段中文内容。" for i in range(500))
        chunks = Chunker(chunk_size=1000).split(text)
        rejoined = "".join(chunks)
        for i in (0, 250, 499):
            assert f"第 {i} 段" in rejoined, f"第 {i} 段在切片中丢失"

    def test_overlap_config_clamped(self):
        """overlap >= chunk_size 会导致无法前进, 必须被夹住。"""
        chunker = Chunker(chunk_size=100, overlap=500)
        assert chunker.overlap < chunker.chunk_size
        assert Chunker(chunk_size=10, overlap=0).overlap == 0

    def test_overlap_zero_allowed(self):
        chunks = Chunker(chunk_size=300, overlap=0).split("中文内容。" * 1000)
        assert len(chunks) > 1
        for c in chunks:
            assert len(c) <= 300

    @pytest.mark.parametrize("size", [1, 7, 50, 999, 4001])
    def test_various_sizes_no_overflow(self, size):
        chunker = Chunker(chunk_size=size)
        chunks = chunker.split("中" * 3000)
        for c in chunks:
            assert len(c) <= size
