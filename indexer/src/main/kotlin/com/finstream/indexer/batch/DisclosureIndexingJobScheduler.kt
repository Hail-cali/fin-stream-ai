package com.finstream.indexer.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DisclosureIndexingJobScheduler(
    private val jobLauncher: JobLauncher,
    private val disclosureIndexingJob: Job
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        cron = "\${indexer.schedule.cron:0 0 2 1 * *}",
        zone = "\${indexer.schedule.zone:UTC}"
    )
    fun runMonthlyIndexingJob() {
        val jobParameters = JobParametersBuilder()
            .addLong("scheduledAt", System.currentTimeMillis())
            .toJobParameters()

        val execution = jobLauncher.run(disclosureIndexingJob, jobParameters)
        log.info("Scheduled indexing job started. executionId={}, status={}", execution.id, execution.status)
    }
}
