from graph.state import GraphState


def alert_node(state: GraphState) -> dict:
    """CRITICAL 리스크 알림 (파일럿: 콘솔 출력)"""
    print("\n" + "=" * 60)
    print("🚨 CRITICAL ALERT 🚨")
    print(f"종목: {state.get('stock_name', '')} ({state.get('stock_code', '')})")
    print(f"뉴스: {state.get('title', '')}")
    print(f"리스크: {state.get('risk_level', '')} — {state.get('risk_reason', '')}")
    print(f"시사점: {state.get('insight', '')}")
    print("=" * 60 + "\n")
    return {}
