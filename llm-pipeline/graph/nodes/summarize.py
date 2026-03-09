from graph.client import get_gemini_llm
from graph.state import GraphState

llm = get_gemini_llm(temperature=0.1)

PROMPT = """당신은 증권 뉴스 분석가입니다.
아래 뉴스를 핵심만 3줄로 요약하세요. 각 줄은 한 문장으로.

[뉴스 제목]
{title}

[뉴스 본문]
{content}

3줄 요약:"""


def summarize_node(state: GraphState) -> dict:
    response = llm.invoke(
        PROMPT.format(title=state["title"], content=state["content"])
    )
    return {"summary": response.content.strip()}
