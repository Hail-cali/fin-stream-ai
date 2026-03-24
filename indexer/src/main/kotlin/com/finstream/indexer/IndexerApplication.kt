package com.finstream.indexer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class IndexerApplication

fun main(args: Array<String>) {
    runApplication<IndexerApplication>(*args)
}
