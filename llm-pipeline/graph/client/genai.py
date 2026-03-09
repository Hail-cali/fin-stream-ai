import os
from functools import lru_cache

from langchain_google_genai import ChatGoogleGenerativeAI



def configure_google_api_key() -> None:
    """GEMINI_API_KEY가 있으면 GOOGLE_API_KEY로 동기화한다."""
    gemini_key = os.getenv("GEMINI_API_KEY", "")
    if gemini_key and not os.getenv("GOOGLE_API_KEY"):
        os.environ["GOOGLE_API_KEY"] = gemini_key


@lru_cache(maxsize=8)
def get_gemini_llm(temperature: float = 0.0) -> ChatGoogleGenerativeAI:
    """공통 Gemini 클라이언트를 생성한다 (temperature 별 캐시)."""
    configure_google_api_key()
    return ChatGoogleGenerativeAI(model="gemini-1.5-flash", temperature=temperature)
