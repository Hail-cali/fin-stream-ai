from graph.state import GraphState
from rag.retriever import retrieve, format_context


def rag_context_node(state: GraphState) -> dict:
    """Qdrant에서 관련 공시를 검색하여 컨텍스트로 조립"""
    query = f"{state['title']} {state['content']}"
    results = retrieve(
        query=query,
        stock_code=state["stock_code"],
        top_k=5,
    )

    rag_context = format_context(results)
    rag_sources = [r["disclosure_id"] for r in results if r["disclosure_id"]]

    return {
        "rag_context": rag_context,
        "rag_sources": rag_sources,
    }
