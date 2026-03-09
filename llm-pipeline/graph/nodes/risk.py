import json

from langchain_google_genai import ChatGoogleGenerativeAI
from graph.state import GraphState

llm = ChatGoogleGenerativeAI(model="gemini-1.5-flash", temperature=0.0)

PROMPT = """당신은 증권 리스크 분석가입니다.
아래 뉴스와 관련 공시를 바탕으로 투자 리스크를 평가하세요.

[뉴스 제목]
{title}

[뉴스 요약]
{summary}

[감성 분석]
{sentiment} (점수: {sentiment_score})

[관련 공시 컨텍스트 — RAG]
{rag_context}

리스크 레벨을 LOW / MEDIUM / HIGH / CRITICAL 중 하나로 판단하고,
구체적인 근거를 제시하세요.

반드시 아래 JSON 형식으로만 응답하세요:
{{"risk_level": "LOW/MEDIUM/HIGH/CRITICAL", "reason": "구체적 근거 1-2문장"}}

JSON:"""


def risk_node(state: GraphState) -> dict:
    response = llm.invoke(
        PROMPT.format(
            title=state["title"],
            summary=state.get("summary", ""),
            sentiment=state.get("sentiment", "NEUTRAL"),
            sentiment_score=state.get("sentiment_score", 0.5),
            rag_context=state.get("rag_context", "관련 공시 없음"),
        )
    )
    try:
        text = response.content.strip()
        if "```" in text:
            text = text.split("```")[1]
            if text.startswith("json"):
                text = text[4:]
            text = text.strip()
        result = json.loads(text)
        risk_level = result["risk_level"].upper()
        return {
            "risk_level": risk_level,
            "risk_reason": result["reason"],
            "is_critical": risk_level == "CRITICAL",
        }
    except (json.JSONDecodeError, KeyError):
        return {"risk_level": "MEDIUM", "risk_reason": "분석 실패", "is_critical": False}
