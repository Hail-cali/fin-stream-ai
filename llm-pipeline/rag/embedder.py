import os
from openai import OpenAI

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
MODEL = "text-embedding-3-small"


def embed_query(text: str) -> list[float]:
    """쿼리 텍스트를 OpenAI 임베딩 벡터로 변환"""
    response = client.embeddings.create(
        model=MODEL,
        input=text,
    )
    return response.data[0].embedding
