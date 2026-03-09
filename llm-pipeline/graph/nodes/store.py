import json
from graph.state import GraphState


def store_node(state: GraphState) -> dict:
    """분석 결과 저장 (파일럿: 콘솔에 JSON 출력)"""
    result = {
        "news_id": state.get("news_id", ""),
        "stock_code": state.get("stock_code", ""),
        "stock_name": state.get("stock_name", ""),
        "title": state.get("title", ""),
        "summary": state.get("summary", ""),
        "sentiment": state.get("sentiment", ""),
        "sentiment_score": state.get("sentiment_score"),
        "risk_level": state.get("risk_level", ""),
        "risk_reason": state.get("risk_reason", ""),
        "insight": state.get("insight", ""),
        "rag_sources": state.get("rag_sources", []),
    }

    print("\n" + "=" * 60)
    print("📊 분석 결과")
    print("=" * 60)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    print("=" * 60 + "\n")

    return {}
