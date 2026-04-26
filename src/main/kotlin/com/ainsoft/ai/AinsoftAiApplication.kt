package com.ainsoft.ai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AinsoftAiApplication

fun main(args: Array<String>) {
    runApplication<AinsoftAiApplication>(*args)
}
