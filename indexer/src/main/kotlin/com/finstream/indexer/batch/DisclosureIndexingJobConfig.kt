package com.finstream.indexer.batch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.finstream.indexer.domain.Disclosure
import com.finstream.indexer.domain.DisclosureChunk
import com.finstream.indexer.domain.EmbeddedChunk
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.support.ListItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class DisclosureIndexingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val chunkingProcessor: ChunkingProcessor,
    private val embeddingProcessor: EmbeddingProcessor,
    private val qdrantWriter: QdrantWriter
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())

    @Bean
    fun disclosureIndexingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .start(indexingStep())
            .build()
    }

    @Bean
    fun indexingStep(): Step {
        return StepBuilder("indexingStep", jobRepository)
            .chunk<Disclosure, List<EmbeddedChunk>>(1, transactionManager)
            .reader(disclosureReader())
            .processor(compositeProcessor())
            .writer(qdrantWriter)
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(3)
            .build()
    }

    // TODO: MOCK READER
    @Bean
    fun disclosureReader(): ListItemReader<Disclosure> {
        val resource = ClassPathResource(MOCK_DATA)
        val disclosures: List<Disclosure> = objectMapper.readValue(resource.inputStream)
        log.info("Loaded {} disclosures from mock data", disclosures.size)
        return ListItemReader(disclosures)
    }

    @Bean
    fun compositeProcessor(): ItemProcessor<Disclosure, List<EmbeddedChunk>> {
        return ItemProcessor { disclosure ->
            val chunks: List<DisclosureChunk> = chunkingProcessor.process(disclosure)
            embeddingProcessor.process(chunks)
        }
    }

    companion object {
        internal const val JOB_NAME = "indexJob"
        const val MOCK_DATA = "mock_disclosures.json"
    }
}
