from pydantic import BaseModel
from typing import List, Optional

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