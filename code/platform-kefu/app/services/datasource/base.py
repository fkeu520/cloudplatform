"""
数据源适配器抽象层（DESIGN §七 扩展架构）

设计要点：
- 每个数据源（faq/knowledge/park-enterprise 等）实现 DataSourceAdapter 接口
- IntentRouter 根据关键词匹配到合适的数据源
- DataSourceRegistry 持有所有适配器实例
- 新增数据源只需 5 步（DESIGN §七.5）：实现 adapter → 注册到 Registry → 插入 kefu_data_source 表 → settings 启用 → UI 自动出现

核心抽象：
- DataSourceAdapter.query(intent, params) → 返回实体引用 + 摘要
- DataSourceAdapter.intent_patterns: List[str] 关键词列表（用于路由匹配）
"""

from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
import re
import json
import logging

logger = logging.getLogger(__name__)


class DataSourceRef:
    """数据源实体引用"""
    def __init__(self, source_id: str, entity_type: str, entity_id: str, label: str, raw: Dict[str, Any] = None):
        self.source_id = source_id
        self.entity_type = entity_type
        self.entity_id = entity_id
        self.label = label
        self.raw = raw or {}

    def to_dict(self) -> Dict[str, Any]:
        return {
            "source": self.source_id,
            "entity_type": self.entity_type,
            "entity_id": self.entity_id,
            "label": self.label,
            "raw": self.raw,
        }


class DataSourceResult:
    """数据源查询结果"""
    def __init__(self, refs: List[DataSourceRef], summary: str = ""):
        self.refs = refs
        self.summary = summary


class DataSourceAdapter(ABC):
    """数据源适配器基类"""

    def __init__(self, id: str, name: str, type: str, intent_keywords: List[str] = None,
                 module_ref: str = None, config: Dict[str, Any] = None):
        self.id = id
        self.name = name
        self.type = type  # internal | http_api | vector_search | database_query
        self.intent_keywords = intent_keywords or []
        self.module_ref = module_ref
        self.config = config or {}
        self.enabled = True

    @abstractmethod
    async def query(self, question: str, params: Dict[str, Any] = None) -> DataSourceResult:
        """查询数据源，返回匹配的实体引用 + 摘要"""
        pass

    def match(self, question: str) -> bool:
        """关键词匹配（路由判断）"""
        if not self.intent_keywords:
            return False
        lower_q = question.lower()
        return any(kw.lower() in lower_q for kw in self.intent_keywords)

    def to_response(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "name": self.name,
            "type": self.type,
            "module_ref": self.module_ref,
            "enabled": self.enabled,
            "sync_strategy": self.config.get("sync_strategy", "realtime"),
            "sync_interval": self.config.get("sync_interval", 0),
            "intent_keywords": self.intent_keywords,
            "last_sync_at": self.config.get("last_sync_at"),
            "last_sync_status": self.config.get("last_sync_status"),
        }


class DataSourceRegistry:
    """数据源注册中心（DESIGN §七.5 step 2）"""

    def __init__(self):
        self._adapters: Dict[str, DataSourceAdapter] = {}

    def register(self, adapter: DataSourceAdapter):
        """注册数据源（重复注册会覆盖）"""
        self._adapters[adapter.id] = adapter
        logger.info(f"[Registry] 注册数据源: {adapter.id} ({adapter.name})")

    def unregister(self, source_id: str):
        self._adapters.pop(source_id, None)

    def get(self, source_id: str) -> Optional[DataSourceAdapter]:
        return self._adapters.get(source_id)

    def all(self) -> List[DataSourceAdapter]:
        return list(self._adapters.values())

    def enabled(self) -> List[DataSourceAdapter]:
        return [a for a in self._adapters.values() if a.enabled]

    async def route_query(self, question: str) -> Dict[str, DataSourceResult]:
        """路由：question → 匹配的 adapters → 各自查询结果

        Returns: { source_id: DataSourceResult }
        """
        results = {}
        matched = [a for a in self.enabled() if a.match(question)]
        if not matched:
            return results
        for adapter in matched:
            try:
                result = await adapter.query(question)
                if result and result.refs:
                    results[adapter.id] = result
                    logger.info(f"[Router] {adapter.id} 命中 {len(result.refs)} 个实体")
            except Exception as e:
                logger.error(f"[Router] {adapter.id} 查询失败: {e}")
        return results


# === 全局 Registry 单例 ===

registry = DataSourceRegistry()