#!/bin/bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "============================================"
echo "  FinStream Intelligence — RAG Pilot"
echo "============================================"

# .env 파일 확인
if [ ! -f "$PROJECT_ROOT/.env" ]; then
    echo "ERROR: .env 파일이 없습니다."
    echo "  cp .env.example .env 후 API 키를 입력하세요."
    exit 1
fi

# 전체 실행 (docker compose가 순서를 관리)
#   1. 인프라 기동 (Kafka, Qdrant, PG, Redis)
#   2. indexer 실행 (공시 → Qdrant 인덱싱) → 완료 후 종료
#   3. llm-pipeline 실행 (mock 뉴스 분석) → 완료 후 종료
echo ""
echo ">>> docker compose up (build + run)..."
docker compose -f "$PROJECT_ROOT/docker-compose.yml" up --build

echo ""
echo "============================================"
echo "  Pilot Complete"
echo "============================================"
echo ""
echo "Qdrant 컬렉션 확인:"
echo "  curl http://localhost:6333/collections/disclosures"
echo ""
echo "인프라 정리:"
echo "  docker compose down"
echo "  docker compose down -v  (볼륨까지 삭제)"
