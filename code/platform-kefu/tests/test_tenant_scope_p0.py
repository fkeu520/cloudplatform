import unittest


class TenantScopeContractTests(unittest.TestCase):
    def test_normalize_tenant_id_accepts_numeric_claim(self):
        from app.core.access import normalize_tenant_id

        self.assertEqual(normalize_tenant_id("7"), 7)

    def test_normalize_tenant_id_rejects_missing_claim(self):
        from app.core.access import normalize_tenant_id

        with self.assertRaises(ValueError):
            normalize_tenant_id(None)

    def test_normalize_tenant_id_rejects_non_integer_values(self):
        from app.core.access import normalize_tenant_id

        with self.assertRaises(ValueError):
            normalize_tenant_id(True)
        with self.assertRaises(ValueError):
            normalize_tenant_id(7.5)

    def test_has_permission_matches_exact_permission(self):
        from app.core.access import has_permission

        user = {"permissions": ["kefu:knowledge:add"]}
        self.assertTrue(has_permission(user, "kefu:knowledge:add"))
        self.assertFalse(has_permission(user, "kefu:knowledge:delete"))
        self.assertFalse(
            has_permission({"permissions": "kefu:knowledge:add-evil"}, "kefu:knowledge:add")
        )

    def test_enterprise_mock_is_fail_closed_for_non_admin_tenant(self):
        from app.services.datasource.park_enterprise_adapter import ParkEnterpriseAdapter

        adapter = ParkEnterpriseAdapter()
        self.assertEqual(adapter._mock_query("企业", 7).refs, [])
        self.assertTrue(adapter._mock_query("企业", 0).refs)


if __name__ == "__main__":
    unittest.main()
