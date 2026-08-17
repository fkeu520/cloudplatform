from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


class AskRequest(BaseModel):
    question: str


class AskResponse(BaseModel):
    ask_id: str
    question: str
    answer: str
    chunks_used: List[str]
    model: str
    latency_ms: int


class DocumentResponse(BaseModel):
    doc_id: str
    name: str
    type: str
    size_bytes: int
    upload_time: str
    status: str
    chunk_count: int


class DocumentListResponse(BaseModel):
    total: int
    items: List[DocumentResponse]


class ChunkResponse(BaseModel):
    chunk_id: str
    doc_id: str
    content: str
    token_count: int
    index_in_doc: int
    vector_id: Optional[int] = None


class AskLogResponse(BaseModel):
    ask_id: str
    question: str
    answer: str
    user_id: Optional[int] = None
    username: Optional[str] = None
    latency_ms: int
    create_time: str


# === 2026-08-17 业务层扩展 ===

class SessionCreateRequest(BaseModel):
    customer_id: Optional[int] = None
    customer_name: Optional[str] = None
    contact: Optional[str] = None
    channel: str = "web"


class SessionResponse(BaseModel):
    id: str
    customer_id: Optional[int]
    customer_name: Optional[str]
    contact: Optional[str]
    channel: str
    status: str
    agent_id: Optional[int]
    agent_name: Optional[str]
    start_time: str
    end_time: Optional[str]
    satisfaction: Optional[int]
    satisfaction_comment: Optional[str]
    last_message_preview: Optional[str]
    last_message_at: Optional[str]


class SessionListResponse(BaseModel):
    total: int
    items: List[SessionResponse]


class MessageCreateRequest(BaseModel):
    content: str
    role: str = "customer"


class MessageResponse(BaseModel):
    msg_id: str
    session_id: str
    role: str
    content: str
    data_source_refs: List[Dict[str, Any]] = Field(default_factory=list)
    knowledge_refs: List[str] = Field(default_factory=list)
    model: Optional[str] = None
    latency_ms: int = 0
    create_time: str


class ChatResponse(BaseModel):
    user_message: MessageResponse
    ai_message: MessageResponse
    session_status: str
    referenced_data_sources: List[Dict[str, Any]] = Field(default_factory=list)


class SessionRateRequest(BaseModel):
    satisfaction: int = Field(..., ge=1, le=5)
    comment: Optional[str] = None


class FaqCreateRequest(BaseModel):
    question: str
    answer: str
    category: str = "其他"


class FaqResponse(BaseModel):
    faq_id: str
    question: str
    answer: str
    category: str
    hit_count: int
    sat_avg: Optional[float]
    enabled: bool
    created_at: str
    updated_at: str


class FaqListResponse(BaseModel):
    total: int
    items: List[FaqResponse]


class DataSourceResponse(BaseModel):
    id: str
    name: str
    type: str
    module_ref: Optional[str]
    enabled: bool
    sync_strategy: str
    sync_interval: int
    intent_keywords: List[str]
    last_sync_at: Optional[str]
    last_sync_status: Optional[str]


class DataSourceUpdateRequest(BaseModel):
    enabled: Optional[bool] = None
    sync_strategy: Optional[str] = None
    intent_keywords: Optional[List[str]] = None
    config_json: Optional[Dict[str, Any]] = None


class DataSourceRegisterRequest(BaseModel):
    id: str
    name: str
    type: str  # internal | http_api | vector_search | database_query
    module_ref: Optional[str] = None
    sync_strategy: str = "realtime"
    intent_keywords: List[str] = Field(default_factory=list)
    config_json: Dict[str, Any] = Field(default_factory=dict)


class DataSourceListResponse(BaseModel):
    total: int
    items: List[DataSourceResponse]


class DataSourceHitResponse(BaseModel):
    session_id: str
    data_source_id: str
    hit_count: int
    last_hit_at: str


class EvaluationRequest(BaseModel):
    msg_id: str
    source_id: Optional[str] = None
    score: int = Field(..., ge=1, le=5)
    comment: Optional[str] = None


class EvaluationResponse(BaseModel):
    eval_id: str
    msg_id: str
    session_id: str
    source_id: Optional[str]
    score: int
    comment: Optional[str]
    created_at: str


class DashboardStatsResponse(BaseModel):
    total_sessions: int
    total_messages: int
    ai_resolution_rate: float
    avg_satisfaction: Optional[float]
    data_source_hit_counts: Dict[str, int]
    recent_24h_sessions: int