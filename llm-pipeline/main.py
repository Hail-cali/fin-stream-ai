"""
FinStream Intelligence — RAG 파일럿 실행 진입점

Kafka 없이 mock 뉴스로 전체 파이프라인(요약→감성→RAG→리스크→인사이트)을 검증한다.

사전 조건:
  1. Qdrant 실행 중 (docker compose -f docker-compose.dev.yml up -d)
  2. Kotlin Indexer 실행 완료 (공시가 Qdrant에 인덱싱된 상태)
  3. .env 파일에 OPENAI_API_KEY, GEMINI_API_KEY 설정

실행:
  cd llm-pipeline
  pip install -r requirements.txt
  python main.py
"""

import os
import sys

from dotenv import load_dotenv

# .env 로드 (프로젝트 루트)
load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))

# GOOGLE_API_KEY 환경변수 설정 (langchain-google-genai 용)
gemini_key = os.getenv("GEMINI_API_KEY", "")
if gemini_key:
    os.environ["GOOGLE_API_KEY"] = gemini_key

from graph.pipeline import build_pipeline
from graph.state import GraphState


# Mock 뉴스 데이터 — 삼성전자 반도체 수출규제 관련
MOCK_NEWS = [
    {
        "news_id": "news-001",
        "stock_code": "005930",
        "stock_name": "삼성전자",
        "title": "미국, 대중 반도체 수출규제 추가 강화… 삼성전자 영향 불가피",
        "content": (
            "미국 상무부는 중국향 첨단 반도체 장비 및 기술 수출 규제를 "
            "대폭 강화하는 새로운 행정명령을 발표했다. 이번 규제에는 "
            "HBM(고대역폭 메모리)과 관련된 첨단 패키징 기술도 포함되어 "
            "삼성전자의 중국 내 반도체 사업에 직접적인 영향을 미칠 전망이다. "
            "삼성전자는 중국 시안 공장에서 NAND 플래시를 생산하고 있으며, "
            "중국 매출 비중이 전체의 약 26%에 달한다. 업계에서는 삼성전자가 "
            "단기적으로 매출 감소 압박을 받을 수 있으나, 미국 내 파운드리 투자 "
            "확대로 중장기적 수혜가 기대된다는 분석이 나온다."
        ),
    },
    {
        "news_id": "news-002",
        "stock_code": "000660",
        "stock_name": "SK하이닉스",
        "title": "SK하이닉스, HBM3E 엔비디아 차세대 GPU 독점 공급 확정",
        "content": (
            "SK하이닉스가 엔비디아의 차세대 AI GPU 'B200'에 탑재될 "
            "HBM3E 메모리를 독점 공급하는 계약을 체결한 것으로 확인됐다. "
            "이에 따라 SK하이닉스의 2025년 HBM 매출은 전년 대비 2배 이상 "
            "성장할 것으로 전망된다. 증권가에서는 SK하이닉스의 AI 반도체 "
            "시장 지배력이 더욱 강화될 것으로 평가하고 있다. "
            "다만, 미국의 대중 수출규제 확대 시 다롄 DRAM 공장 운영에 "
            "제약이 발생할 수 있다는 점은 리스크 요인으로 지적된다."
        ),
    },
]


def main():
    print("=" * 60)
    print("🚀 FinStream Intelligence — RAG 파일럿 실행")
    print("=" * 60)

    pipeline = build_pipeline()

    for news in MOCK_NEWS:
        print(f"\n📰 분석 시작: {news['title']}")
        print("-" * 60)

        initial_state: GraphState = {
            "news_id": news["news_id"],
            "stock_code": news["stock_code"],
            "stock_name": news["stock_name"],
            "title": news["title"],
            "content": news["content"],
        }

        result = pipeline.invoke(initial_state)

        print(f"\n✅ 분석 완료: {news['stock_name']}")
        print(f"   요약: {result.get('summary', 'N/A')[:80]}...")
        print(f"   감성: {result.get('sentiment', 'N/A')} ({result.get('sentiment_score', 'N/A')})")
        print(f"   리스크: {result.get('risk_level', 'N/A')}")
        print(f"   RAG 참조: {len(result.get('rag_sources', []))}건 공시")
        print(f"   인사이트: {result.get('insight', 'N/A')}")

    print("\n" + "=" * 60)
    print("🏁 파일럿 실행 완료")
    print("=" * 60)


if __name__ == "__main__":
    main()
