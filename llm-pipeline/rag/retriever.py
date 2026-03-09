import os
from datetime import datetime, timedelta, timezone

from qdrant_client import QdrantClient
from qdrant_client.models import Filter, FieldCondition, MatchValue

from rag.embedder import embed_query

QDRANT_HOST = os.getenv("QDRANT_HOST", "localhost")
QDRANT_PORT = int(os.getenv("QDRANT_GRPC_PORT", "6334"))
COLLECTION_NAME = os.getenv("QDRANT_COLLECTION", "disclosures")


def _get_client() -> QdrantClient:
    api_key = os.getenv("QDRANT_API_KEY", None)
    return QdrantClient(
        host=QDRANT_HOST,
        port=QDRANT_PORT,
        api_key=api_key if api_key else None,
        prefer_grpc=True,
    )


def retrieve(
    query: str,
    stock_code: str,
    top_k: int = 5,
) -> list[dict]:
    """
    뉴스 텍스트로 관련 공시 청크를 Qdrant에서 검색.

    Returns:
        list[dict]: 각 항목은 {chunk_text, score, disclosure_id, title} 포함
    """
    query_vector = embed_query(query)
    client = _get_client()

    # stock_code 필터 — 같은 종목 공시만 검색 (RAG 정확도 핵심)
    search_filter = Filter(
        must=[
            FieldCondition(
                key="stock_code",
                match=MatchValue(value=stock_code),
            )
        ]
    )

    results = client.query_points(
        collection_name=COLLECTION_NAME,
        query=query_vector,
        query_filter=search_filter,
        limit=top_k,
        with_payload=True,
    )

    return [
        {
            "chunk_text": point.payload.get("chunk_text", ""),
            "score": point.score,
            "disclosure_id": point.payload.get("disclosure_id", ""),
            "title": point.payload.get("title", ""),
            "disclosed_at": point.payload.get("disclosed_at", ""),
        }
        for point in results.points
    ]


def format_context(results: list[dict]) -> str:
    """검색 결과를 LLM 프롬프트에 주입할 컨텍스트 문자열로 조립"""
    if not results:
        return "관련 공시 없음"

    parts = []
    for i, r in enumerate(results, 1):
        parts.append(
            f"[관련 공시 {i} - 유사도 {r['score']:.2f}]\n"
            f"제목: {r['title']}\n"
            f"공시일: {r['disclosed_at']}\n"
            f"{r['chunk_text']}"
        )
    return "\n\n".join(parts)
