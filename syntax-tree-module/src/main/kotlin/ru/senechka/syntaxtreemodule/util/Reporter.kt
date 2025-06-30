package ru.senechka.syntaxtreemodule.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Reporter {

    @Serializable
    data class ViolationResult(
        val className: String,
        val lineNumber: Int,
        val messages: List<String>
    )

    @Serializable
    data class AnalysisResult(
        val analysis: String,
        val violations: List<ViolationResult>
    )

    fun printViolations(analysisName: String, violations: List<ViolationResult>) {
        println("=== Анализ: $analysisName ===")
        if (violations.isEmpty()) {
            println("✅ Нарушений не найдено.")
        } else {
            violations.forEach { v ->
                println("❌ Класс ${v.className} (строка ${v.lineNumber}):")
                v.messages.forEach { msg ->
                    println("   - $msg")
                }
            }
        }
        println()
    }

    fun toJson(analysisName: String, violations: List<ViolationResult>): String {
        val result = AnalysisResult(analysisName, violations)
        return Json.encodeToString(result)
    }

    // Специальные методы для других типов анализа (например, композиция, метрики и т.д.)

    fun printSimpleTable(
        analysisName: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        println("=== Анализ: $analysisName ===")
        if (rows.isEmpty()) {
            println("✅ Данных не найдено.")
            return
        }
        val colWidths = headers.mapIndexed { i, h ->
            maxOf(h.length, rows.maxOfOrNull { it.getOrNull(i)?.length ?: 0 } ?: 0)
        }
        val format = colWidths.joinToString(" | ", prefix = "| ", postfix = " |") { "%-${it}s" }

        println(format.format(*headers.toTypedArray()))
        println(colWidths.joinToString("-+-", prefix = "|-", postfix = "-|") { "".padEnd(it, '-') })
        rows.forEach { row ->
            println(format.format(*row.toTypedArray()))
        }
        println()
    }
}
