import json

from graph.client import get_gemini_llm
from graph.state import GraphState

llm = get_gemini_llm(temperature=0.0)

PROMPT = """당신은 증권 뉴스 감성 분석가입니다.
아래 뉴스의 감성을 분석하세요.

[뉴스 제목]
{title}

[뉴스 요약]
{summary}

반드시 아래 JSON 형식으로만 응답하세요:
{{"sentiment": "POSITIVE" 또는 "NEGATIVE" 또는 "NEUTRAL", "score": 0.0~1.0 사이 소수}}

JSON:"""


def sentiment_node(state: GraphState) -> dict:
    response = llm.invoke(
        PROMPT.format(title=state["title"], summary=state.get("summary", ""))
    )
    try:
        text = response.content.strip()
        # JSON 블록 추출
        if "```" in text:
            text = text.split("```")[1]
            if text.startswith("json"):
                text = text[4:]
            text = text.strip()
        result = json.loads(text)
        return {
            "sentiment": result["sentiment"],
            "sentiment_score": float(result["score"]),
        }
    except (json.JSONDecodeError, KeyError):
        return {"sentiment": "NEUTRAL", "sentiment_score": 0.5}
