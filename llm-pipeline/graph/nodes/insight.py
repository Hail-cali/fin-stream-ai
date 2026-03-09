from langchain_google_genai import ChatGoogleGenerativeAI
from graph.state import GraphState

llm = ChatGoogleGenerativeAI(model="gemini-1.5-flash", temperature=0.2)

PROMPT = """당신은 증권 투자 인사이트 전문가입니다.
아래 분석 결과를 바탕으로 1줄 투자 시사점을 작성하세요.

[종목]
{stock_name} ({stock_code})

[뉴스 요약]
{summary}

[감성]
{sentiment} ({sentiment_score})

[리스크]
{risk_level} — {risk_reason}

[관련 공시 컨텍스트]
{rag_context}

투자 시사점 (1줄):"""


def insight_node(state: GraphState) -> dict:
    response = llm.invoke(
        PROMPT.format(
            stock_name=state.get("stock_name", ""),
            stock_code=state.get("stock_code", ""),
            summary=state.get("summary", ""),
            sentiment=state.get("sentiment", ""),
            sentiment_score=state.get("sentiment_score", 0.5),
            risk_level=state.get("risk_level", ""),
            risk_reason=state.get("risk_reason", ""),
            rag_context=state.get("rag_context", ""),
        )
    )
    return {"insight": response.content.strip()}
