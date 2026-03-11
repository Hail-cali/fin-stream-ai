#!/bin/bash
set -e

MODE="${MODE:-pilot}"

echo "=== LLM Pipeline starting in '${MODE}' mode ==="

case "$MODE" in
  pilot)
    echo "Running pilot (mock news → analyze → exit)"
    python main.py
    ;;
  stream)
    echo "Running Kafka consumer (enriched.news → analyze → analyzed.news)"
    python consumer.py
    ;;
  *)
    echo "Unknown MODE: $MODE (use 'pilot' or 'stream') default stream"
    python consumer.py
    ;;
esac
