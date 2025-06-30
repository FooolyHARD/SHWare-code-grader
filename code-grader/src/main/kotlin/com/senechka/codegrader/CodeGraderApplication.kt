package com.senechka.codegrader

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CodeGraderApplication

fun main(args: Array<String>) {
    runApplication<CodeGraderApplication>(*args)
}
