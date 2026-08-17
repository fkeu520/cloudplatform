"""Basic tests for platform-kefu"""
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health():
    resp = client.get("/api/kefu/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "UP"
    assert data["service"] == "platform-kefu"


def test_create_session():
    resp = client.post("/api/kefu/sessions", json={"customer_name": "Test Customer"})
    assert resp.status_code == 200
    data = resp.json()
    assert "id" in data
    assert data["customer_name"] == "Test Customer"
    assert data["status"] == "AI"


def test_list_data_sources():
    resp = client.get("/api/kefu/data_sources")
    assert resp.status_code == 200
    data = resp.json()
    assert "total" in data
    assert isinstance(data["items"], list)
    # at least the seeded sources
    assert data["total"] >= 3
    ids = [ds["id"] for ds in data["items"]]
    assert "faq" in ids
    assert "knowledge" in ids
    assert "park-enterprise" in ids