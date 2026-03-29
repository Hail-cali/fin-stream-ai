# FinStream Intelligence — System Architecture

## 1. 전체 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         EC2 t4g.medium (4GB RAM)                           │
│                         docker-compose (단일 호스트)                         │
│                                                                             │
│  ┌──────────────┐                                                           │
│  │   Collector   │ Kotlin / Spring Boot 3.3.5                               │
│  │   (256MB)     │ @Scheduled 10분 주기                                     │
│  │              │                                                           │
│  │ ┌──────────┐ │         ┌─────────────────────────────────────────────┐   │
│  │ │DART 공시  │─┤────────▶│              Apache Kafka 3.8              │   │
│  │ │Collector  │ │  raw.   │              KRaft 모드 (192MB)            │   │
│  │ └──────────┘ │  news    │                                             │   │
│  │ ┌──────────┐ │         │  ┌──────────┐  ┌───────────┐  ┌──────────┐ │   │
│  │ │RSS 뉴스   │─┘────────▶│  │ raw.news │  │enriched.  │  │analyzed. │ │   │
│  │ │Collector  │           │  │          │  │news       │  │news      │ │   │
│  │ └──────────┘           │  └─────┬────┘  └─────┬─────┘  └────┬─────┘ │   │
│  └──────────────┘         └────────┼─────────────┼──────────────┼───────┘   │
│                                    │             │              │           │
│                                    ▼             │              │           │
│  ┌─────────────────────────────────────────┐     │              │           │
│  │          Apache Flink 1.18              │     │              │           │
│  │  JobManager (640MB) + TaskManager (640MB)│     │              │           │
│  │                                         │     │              │           │
│  │  raw.news ──▶ StockCodeExtractor (NER)  │     │              │           │
│  │          ──▶ DeduplicationFilter (24h)   │     │              │           │
│  │          ──▶ ImportanceScorer            │     │              │           │
│  │          ──▶ filter(score ≥ 0.3) ───────┼─────┘              │           │
│  │                           enriched.news │                    │           │
│  └─────────────────────────────────────────┘                    │           │
│                                    │                            │           │
│                                    ▼                            │           │
│  ┌─────────────────────────────────────────┐                    │           │
│  │       LLM Pipeline (256MB)              │                    │           │
│  │       Python / LangGraph                │                    │           │
│  │                                         │                    │           │
│  │  enriched.news ──▶ Summarize            │                    │           │
│  │               ──▶ Sentiment             │                    │           │
│  │               ──▶ RAG Context ◀── Qdrant│                    │           │
│  │               ──▶ Risk Assessment       │                    │           │
│  │               ──▶ Insight               │                    │           │
│  │               ──▶ Alert / Store ─────────┼────────────────────┘           │
│  │                          analyzed.news  │                                │
│  └─────────────────────────────────────────┘                                │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────┐    ┌────────────────────┐      │
│  │        API Server (256MB)               │    │   Qdrant (128MB)   │      │
│  │        Kotlin / Spring WebFlux          │◀──▶│   Vector DB        │      │
│  │                                         │    │   gRPC :6334       │      │
│  │  analyzed.news ──▶ In-Memory Cache      │    │   1536dim, Cosine  │      │
│  │                ──▶ SSE Broadcast        │    └────────────────────┘      │
│  │                                         │              ▲                 │
│  │  REST API (:8080)                       │              │                 │
│  │  ├── GET  /api/stocks/{code}/analysis   │    ┌────────────────────┐      │
│  │  ├── GET  /api/stocks/{code}/latest     │    │  Indexer (256MB)   │      │
│  │  ├── GET  /api/stocks/recent            │    │  Kotlin / Batch    │      │
│  │  ├── POST /api/search (RAG)             │    │  mock_disclosures  │      │
│  │  ├── GET  /api/stream (SSE)             │    │  ──▶ Chunk+Embed   │      │
│  │  └── GET  /api/health                   │    │  ──▶ Qdrant 저장   │      │
│  └──────────────┬──────────────────────────┘    │  (완료 후 종료)     │      │
│                 │                               └────────────────────┘      │
└─────────────────┼──────────────────────────────────────────────────────────┘
                  │ :8080
                  ▼
┌─────────────────────────────────────────┐
│        Frontend (Vercel)                │
│        Next.js 14 + Tailwind CSS        │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │  Dashboard (/)                    │  │
│  │  ├── 실시간 분석 피드 (SSE)         │  │
│  │  ├── 리스크 레벨 필터              │  │
│  │  └── RAG 검색                     │  │
│  ├───────────────────────────────────┤  │
│  │  종목 상세 (/stocks/[code])       │  │
│  │  └── 분석 히스토리                 │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

## 2. 데이터 파이프라인 흐름

```
   외부 데이터                Kafka 토픽              처리 서비스           출력
  ─────────────          ──────────────          ──────────────       ──────────

  DART OpenAPI ──┐
  (공시 메타)     │
                 ├──▶  raw.news  ──▶  Flink   ──▶  enriched.news  ──▶  LLM Pipeline
  뉴스 RSS ──────┘     (원본 뉴스)     (전처리)      (정제 뉴스)          │
  (한경/매경 등)                                                        │
                                                                       ▼
                                                                  analyzed.news
                                                                  (분석 결과)
                                                                       │
                                      Qdrant ◀── RAG 검색              │
                                      (공시 벡터)                       ▼
                                                                  API Server
                                                                  (REST + SSE)
                                                                       │
                                                                       ▼
                                                                  Frontend
                                                                  (Dashboard)
```

---

## 3. Kafka 토픽 설계

| 토픽 | Producer | Consumer | 파티션 | 보존 |
|------|----------|----------|--------|------|
| `raw.news` | Collector | Flink | 1 | 12h |
| `enriched.news` | Flink | LLM Pipeline | 1 | 12h |
| `analyzed.news` | LLM Pipeline | API Server | 1 | 12h |

### 메시지 스키마

```
raw.news
├── news_id: String        (dart-{rceptNo} / rss-{hash})
├── stock_code: String     (6자리, e.g. "005930")
├── stock_name: String     (e.g. "삼성전자")
├── title: String
├── content: String
├── source: String         (DART / RSS)
└── published_at: ISO8601

enriched.news
├── news_id: String
├── stock_code: String
├── stock_name: String
├── title: String
├── content: String
└── importance_score: Float  (0.0 ~ 1.0, threshold ≥ 0.3)

analyzed.news
├── news_id: String
├── stock_code: String
├── stock_name: String
├── title: String
├── summary: String
├── sentiment: String       (POSITIVE / NEUTRAL / NEGATIVE)
├── sentiment_score: Float  (-1.0 ~ 1.0)
├── risk_level: String      (LOW / MEDIUM / HIGH / CRITICAL)
├── risk_reason: String
├── insight: String
└── rag_sources: List<String>
```

---

## 4. 서비스별 상세

### 4-1. Collector (Kotlin / Spring Boot)

```
@Scheduled(10분)
├── DartCollector
│   ├── DART OpenAPI /api/list.json 호출
│   ├── 주요 10개 종목 stockNameMap 기반 필터
│   ├── processedIds (in-memory) 중복 방지
│   └── KafkaNewsProducer → raw.news
│
└── RssCollector
    ├── RSS 피드 파싱 (ROME 라이브러리)
    ├── stockDictionary 기반 NER (14개 키워드)
    ├── processedLinks (10,000건 cap)
    └── KafkaNewsProducer → raw.news
```

### 4-2. Flink Job (Kotlin / Flink 1.18)

```
KafkaSource(raw.news)
  │
  ▼
StockCodeExtractor ─── 사전 기반 NER (20개 종목)
  │                     제목+본문에서 종목명 매칭
  ▼                     종목코드 보정/할당
filter(stockCode.length == 6)
  │
  ▼
DeduplicationFilter ── news_id 기반 KeyedState
  │                     TTL 24시간 자동 만료
  ▼                     동일 뉴스 재처리 방지
ImportanceScorer ───── 키워드 가중치 스코어링
  │                     실적/배당/M&A 등 → 0.0~1.0
  ▼
filter(score ≥ 0.3)
  │
  ▼
KafkaSink(enriched.news)
```

### 4-3. LLM Pipeline (Python / LangGraph)

```
KafkaConsumer(enriched.news)
  │
  ▼
LangGraph StateGraph DAG
  │
  ├──▶ summarize ──── Gemini 1.5 Flash (한국어 요약)
  │
  ├──▶ sentiment ──── 감성 분석 (POSITIVE/NEUTRAL/NEGATIVE + 점수)
  │
  ├──▶ rag_context ── Qdrant 벡터 검색 (관련 공시 컨텍스트)
  │                    OpenAI text-embedding-3-small
  │
  ├──▶ risk ───────── 리스크 평가 (LOW/MEDIUM/HIGH/CRITICAL)
  │
  ├──▶ insight ────── 투자 인사이트 생성
  │
  └──▶ alert/store ── CRITICAL → 알림 / else → 저장
         │
         ▼
    KafkaProducer(analyzed.news)
```

### 4-4. API Server (Kotlin / Spring WebFlux)

```
┌─────────────────────────────────────────────────┐
│  AnalyzedNewsConsumer                            │
│  @KafkaListener(analyzed.news)                   │
│  └── AnalysisService.save() + RealtimeService   │
├─────────────────────────────────────────────────┤
│  AnalysisService                                 │
│  ConcurrentHashMap<stockCode, Deque<Analysis>>  │
│  최대 100건/종목, 500건/전체 (in-memory)          │
├─────────────────────────────────────────────────┤
│  SearchService                                   │
│  OpenAI Embedding → Qdrant 벡터 검색             │
│  stock_code 필터 지원                            │
├─────────────────────────────────────────────────┤
│  RealtimeService                                 │
│  Sinks.many().multicast().onBackpressureBuffer  │
│  → Flux<ServerSentEvent> 브로드캐스트             │
├─────────────────────────────────────────────────┤
│  REST Endpoints                                  │
│  GET  /api/stocks/{code}/analysis     종목 분석   │
│  GET  /api/stocks/{code}/analysis/latest         │
│  GET  /api/stocks/recent              최신 전체   │
│  POST /api/search                     RAG 검색   │
│  GET  /api/stream                     SSE 스트림  │
│  GET  /api/health                     헬스체크    │
└─────────────────────────────────────────────────┘
```

### 4-5. Indexer (Kotlin / Spring Batch)

```
mock_disclosures.json (3건)
  │
  ▼
ChunkingProcessor ── 텍스트 청킹 (500자 + 100자 오버랩)
  │
  ▼
EmbeddingProcessor ── OpenAI text-embedding-3-small (1536dim)
  │
  ▼
QdrantWriter ────── Qdrant "disclosures" 컬렉션 upsert
                     UUID v5 (동일 청크 중복 방지)
                     payload: disclosure_id, stock_code, title, chunk_text

  ※ 배치 완료 후 컨테이너 종료 → 메모리 반환
```

### 4-6. Frontend (Next.js 14 / Vercel)

```
Pages
├── / (Dashboard)
│   ├── AnalysisFeed ── SSE EventSource 연결 → 실시간 피드
│   ├── RiskBadge ───── LOW(초록)/MEDIUM(노랑)/HIGH(주황)/CRITICAL(빨강)
│   ├── SentimentGauge ── 감성 점수 프로그레스 바
│   ├── SearchBar ────── RAG 검색 (쿼리 + 종목코드 필터)
│   └── SearchResults ── 유사도 스코어 + 공시 텍스트
│
└── /stocks/[code] (종목 상세)
    ├── 분석 히스토리 목록
    └── StockCard 컴포넌트
```

---

## 5. 서비스 의존관계 (기동 순서)

```
kafka (healthcheck) ──────────────────┐
                                      │
qdrant (healthcheck) ─────────────────┤
         │                            │
         ▼                            ▼
      indexer ──────────┐         collector
    (배치 후 종료)        │
         │              │         flink-jobmanager ──▶ flink-taskmanager
         ▼              │
    llm-pipeline ◀──────┘
    (indexer 완료 후 시작)
                              api (kafka + qdrant healthy)
```

**docker-compose depends_on 정리:**

| 서비스 | 의존 대상 | 조건 |
|--------|----------|------|
| indexer | qdrant | service_healthy |
| collector | kafka | service_healthy |
| flink-jobmanager | kafka | service_healthy |
| flink-taskmanager | flink-jobmanager | service_started |
| llm-pipeline | qdrant, kafka, indexer | healthy, healthy, completed |
| api | kafka, qdrant | service_healthy |

---

## 6. 포트 매핑

| 서비스 | 내부 포트 | 외부 포트 | 용도 |
|--------|----------|----------|------|
| Kafka | 9092 | 9092 | 메시지 브로커 |
| Qdrant | 6333, 6334 | 6333, 6334 | REST, gRPC |
| Flink UI | 8081 | 8081 | Flink 대시보드 |
| API Server | 8080 | 8080 | REST + SSE |
| Frontend | 3000 | — | Vercel 배포 (로컬 dev만) |

---

## 7. 메모리 예산 (t4g.medium = 4GB)

```
┌─────────────────────────────────────────────────────────────┐
│                    4GB Total RAM                             │
│                                                              │
│  ┌─────┐ ┌───┐ ┌─────┐ ┌──────────┐ ┌──────────┐           │
│  │Kafka│ │ Q │ │Coll.│ │  Flink   │ │  Flink   │           │
│  │192MB│ │128│ │256MB│ │  JM      │ │  TM      │           │
│  │     │ │MB │ │     │ │  640MB   │ │  640MB   │           │
│  └─────┘ └───┘ └─────┘ └──────────┘ └──────────┘           │
│                                                              │
│  ┌─────┐ ┌─────┐ ┌─────┐                                    │
│  │ LLM │ │ API │ │ OS  │                                    │
│  │256MB│ │256MB│ │~500M│                                    │
│  └─────┘ └─────┘ └─────┘                                    │
│                                                              │
│  상시 합계: ~2.4GB │ 피크(+Indexer): ~2.6GB │ 여유: ~900MB   │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. 외부 API 의존성

| API | 사용처 | 용도 | 인증 |
|-----|--------|------|------|
| DART OpenAPI | Collector | 공시 목록 수집 | DART_API_KEY |
| OpenAI API | Indexer, API Server | text-embedding-3-small 임베딩 | OPENAI_API_KEY |
| Gemini API | LLM Pipeline | 요약/감성/리스크/인사이트 생성 | GEMINI_API_KEY |

---

## 9. CI/CD 파이프라인

```
GitHub Repository
  │
  ├── PR → main
  │   └── ci.yml (GitHub Actions)
  │       ├── Build Indexer      (gradlew build -x test)
  │       ├── Build Collector    (gradlew build -x test)
  │       ├── Build Flink Job    (gradlew shadowJar)
  │       ├── Build API          (gradlew build -x test)
  │       ├── Build Frontend     (npm ci && npm run build)
  │       └── Docker Compose Build (docker compose build)
  │
  └── Push → main (merge)
      └── deploy.yml (GitHub Actions)
          └── SSH → EC2
              ├── git pull origin main
              ├── docker compose down
              ├── docker compose up --build -d
              └── curl /api/health (헬스체크)
```

---

## 10. 기술 스택 요약

```
┌──────────────────────────────────────────────────────────┐
│  Language    │  Kotlin 1.9.25 │ Python 3.11+ │ TS (Next) │
├──────────────────────────────────────────────────────────┤
│  Framework   │  Spring Boot 3.3.5 (WebFlux, Batch)       │
│              │  Flink 1.18 (DataStream API)               │
│              │  LangGraph (StateGraph)                     │
│              │  Next.js 14 (App Router)                    │
├──────────────────────────────────────────────────────────┤
│  Messaging   │  Apache Kafka 3.8 (KRaft, no Zookeeper)   │
├──────────────────────────────────────────────────────────┤
│  Storage     │  Qdrant (Vector DB, gRPC, 1536dim)         │
│              │  In-Memory Cache (API, MVP)                 │
│              │  H2 (Spring Batch metadata)                 │
├──────────────────────────────────────────────────────────┤
│  AI/ML       │  Gemini 1.5 Flash (LLM)                    │
│              │  OpenAI text-embedding-3-small (임베딩)      │
├──────────────────────────────────────────────────────────┤
│  Infra       │  Docker Compose (단일 호스트)                │
│              │  EC2 t4g.medium (ARM, 4GB, ~$24/월)         │
│              │  Vercel (Frontend CDN)                      │
│              │  GitHub Actions (CI/CD)                     │
└──────────────────────────────────────────────────────────┘
```
