package com.finstream.flink.function

import com.finstream.flink.model.RawNews
import org.apache.flink.api.common.state.StateTtlConfig
import org.apache.flink.api.common.state.ValueState
import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.common.time.Time
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

/**
 * news_id 기반 중복 제거.
 * Flink Keyed State + TTL(24시간)로 동일 뉴스 재처리 방지.
 */
class DeduplicationFilter : KeyedProcessFunction<String, RawNews, RawNews>() {

    private lateinit var seenState: ValueState<Boolean>

    override fun open(parameters: Configuration) {
        val ttlConfig = StateTtlConfig.newBuilder(Time.hours(24))
            .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
            .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
            .build()

        val descriptor = ValueStateDescriptor("seen", Boolean::class.java).apply {
            enableTimeToLive(ttlConfig)
        }

        seenState = runtimeContext.getState(descriptor)
    }

    override fun processElement(
        news: RawNews,
        ctx: Context,
        out: Collector<RawNews>
    ) {
        if (seenState.value() != true) {
            seenState.update(true)
            out.collect(news)
        }
        // 이미 본 뉴스 → 무시
    }
}
