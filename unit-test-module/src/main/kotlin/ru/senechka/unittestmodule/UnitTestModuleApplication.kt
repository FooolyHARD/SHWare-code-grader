package ru.senechka.unittestmodule

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnitTestModuleApplication

fun main(args: Array<String>) {
    runApplication<UnitTestModuleApplication>(*args)
}
