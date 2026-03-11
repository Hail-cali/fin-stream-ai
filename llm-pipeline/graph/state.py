from typing import TypedDict, Optional


class GraphState(TypedDict, total=False):
    # 입력 필드
    news_id: str
    stock_code: str
    stock_name: str
    title: str
    content: str

    # 각 노드가 채워나가는 필드
    summary: Optional[str]
    sentiment: Optional[str]
    sentiment_score: Optional[float]
    rag_context: Optional[str]
    rag_sources: Optional[list[str]]
    risk_level: Optional[str]
    risk_reason: Optional[str]
    insight: Optional[str]
    is_critical: bool
    error: Optional[str]
