"""回填脚本的纯 diff 逻辑单元测试 (P0)

不连 DB、不读环境变量。直接对 ``compute_token_fixes`` 喂伪造行, 断言:
1. 旧值为 1 的中文块被修正为正确的宽字符 token 数。
2. 已正确的块不会被列入变更列表。
3. 空串返回 0; 纯空白块估算 2, 旧值 0 时也应修正。
4. 再次运行已修正后的结果 → 无变更 (幂等)。
"""
import sys
import os

# 让 ``from app.services.chunker import Chunker`` 在任意 cwd 下都可用
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "app"))

from scripts.backfill_token_counts import compute_token_fixes  # noqa: E402


class TestComputeTokenFixes:
    def test_chinese_old_one_becomes_char_count(self):
        """旧实现 len(text.split()) 对中文恒为 1, 新实现按宽字符计。"""
        text = "中文估算" * 100  # 400 字
        rows = [("c1", text, 1)]
        fixes = compute_token_fixes(rows)
        assert len(fixes) == 1, "必须报告变更"
        chunk_id, new_count = fixes[0]
        assert chunk_id == "c1"
        assert new_count >= 400, f"中文 400 字 token 估算应 >= 400, 却得到 {new_count}"

    def test_already_correct_is_omitted(self):
        """新值与旧值一致的行应被跳过。"""
        text = "中文估算" * 100
        chunker = __import__("app.services.chunker", fromlist=["Chunker"]).Chunker()
        correct = chunker.estimate_tokens(text)
        rows = [("c2", text, correct)]
        fixes = compute_token_fixes(rows)
        assert fixes == [], "已正确不应出现在变更列表"

    def test_empty_and_whitespace(self):
        """空串 token=0; 纯空白块估算 2token (4空格+2换行, 6窄字符 ceil(6/4)=2),
        旧值 0 时也应修正。"""
        # 空串: 估算 0, 旧值 0 → 不改
        rows: list[tuple[str, str, int]] = [("c3a", "", 0)]
        fixes = compute_token_fixes(rows)
        assert fixes == [], "空串估算 0, 旧值 0, 不应变更"

        # 纯空白: 估算 2, 旧值 0 → 应修正为 2
        rows_ws = [("c3b", "   \n\n  ", 0)]
        fixes_ws = compute_token_fixes(rows_ws)
        assert len(fixes_ws) == 1
        assert fixes_ws[0] == ("c3b", 2)

        # 旧值非 0 的空白块也应被修正为 2
        rows_dirty = [("c3c", "   \n\n  ", 5)]
        fixes_dirty = compute_token_fixes(rows_dirty)
        assert len(fixes_dirty) == 1
        assert fixes_dirty[0] == ("c3c", 2)

    def test_idempotency_roundtrip(self):
        """对 compute_token_fixes 的结果再跑一次 → 必须为空。"""
        text = "中文估算" * 100
        rows = [("c4", text, 1)]
        fixes_first = compute_token_fixes(rows)
        assert len(fixes_first) == 1
        chunk_id, new_count = fixes_first[0]

        # 模拟"回填后"的状态: 新值已写入, 旧值=新值
        fixes_second = compute_token_fixes([(chunk_id, text, new_count)])
        assert fixes_second == [], "第二次运行应零变更"

    def test_ascii_denser_than_cjk_asserted_by_estimate(self):
        """同长度下, 英文 token 数应 < 中文 (4 字符 per token vs 1 字符 per token)。"""
        en_text = "a" * 400
        cn_text = "中" * 400
        rows = [
            ("c_en", en_text, 100),  # 旧值 (len(en.split())) 是 1, 这里故意给 100 表示失真
            ("c_cn", cn_text, 1),
        ]
        fixes = compute_token_fixes(rows)
        # 中文必变 (旧 1 → 400)
        cn_fix = [f for f in fixes if f[0] == "c_cn"]
        assert len(cn_fix) == 1 and cn_fix[0][1] == 400

        # 英文 400 字符 = 100 token (400/4), 旧值 100 匹配 → 不变更
        en_fix = [f for f in fixes if f[0] == "c_en"]
        assert en_fix == [], "ascii 400 字符 = 100 token, 旧 100 匹配, 不应变更"

    def test_mixed_text(self):
        """中英混合的文本估算值应介于纯中文与纯英文之间。"""
        text = "Hello 世界"  # 5 ascii + 2 wide = ceil(5/4) + 2 = 2+2 = 4
        chunker = __import__("app.services.chunker", fromlist=["Chunker"]).Chunker()
        expected = chunker.estimate_tokens(text)
        rows = [("c_mix", text, 1)]
        fixes = compute_token_fixes(rows)
        assert len(fixes) == 1 and fixes[0][1] == expected
