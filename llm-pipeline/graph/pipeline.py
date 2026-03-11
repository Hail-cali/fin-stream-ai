from langgraph.graph import StateGraph, END

from graph.state import GraphState
from graph.nodes.summarize import summarize_node
from graph.nodes.sentiment import sentiment_node
from graph.nodes.rag_context import rag_context_node
from graph.nodes.risk import risk_node
from graph.nodes.insight import insight_node
from graph.nodes.alert import alert_node
from graph.nodes.store import store_node


def route_after_insight(state: GraphState) -> str:
    """리스크가 CRITICAL이면 alert, 아니면 store"""
    if state.get("is_critical", False):
        return "alert"
    return "store"


def build_pipeline() -> StateGraph:
    """LangGraph DAG 조립"""
    graph = StateGraph(GraphState)

    # 노드 등록
    graph.add_node("summarize", summarize_node)
    graph.add_node("sentiment", sentiment_node)
    graph.add_node("rag_context", rag_context_node)
    graph.add_node("risk", risk_node)
    graph.add_node("insight", insight_node)
    graph.add_node("alert", alert_node)
    graph.add_node("store", store_node)

    # 엣지 연결: summarize → sentiment → rag_context → risk → insight → [alert|store]
    graph.set_entry_point("summarize")
    graph.add_edge("summarize", "sentiment")
    graph.add_edge("sentiment", "rag_context")
    graph.add_edge("rag_context", "risk")
    graph.add_edge("risk", "insight")

    # conditional edge: CRITICAL → alert, 나머지 → store
    graph.add_conditional_edges(
        "insight",
        route_after_insight,
        {"alert": "alert", "store": "store"},
    )

    graph.add_edge("alert", END)
    graph.add_edge("store", END)

    return graph.compile()
