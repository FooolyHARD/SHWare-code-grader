package ru.senechka.staticanalyzemodule

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import ru.senechka.staticmodule.runner.SpotBugsRunner
import java.io.File

@SpringBootApplication
class StaticAnalyzeModuleApplication

@Serializable
data class StaticAnalysisResult(
    val spotbugs: String,
    val checkstyle: String?
)
fun main(args: Array<String>) {
    val classDir = File("/home/arseny/repos/newdex-code-grader-service/syntax-tree-module/source-code/classes/")
    val sourceDir = File("/home/arseny/repos/newdex-code-grader-service/syntax-tree-module/source-code/")
    val configFile = File("/home/arseny/repos/newdex-code-grader-service/static-analyze-module/src/main/kotlin/ru/senechka/staticanalyzemodule/config/checkstyle.xml")

    println("▶ Анализ начинается...")

    val spotBugsJson = SpotBugsRunner.analyze(classDir)
    val checkstyleJson = if (configFile.exists()) {
        CheckstyleRunner.analyze(sourceDir, configFile)
    } else null

    val result = StaticAnalysisResult(
        spotbugs = spotBugsJson,
        checkstyle = checkstyleJson
    )

    val output = Json.encodeToString(result)
    File("static-analysis-result.json").writeText(output)

    println("✅ Анализ завершён. Результат сохранён в static-analysis-result.json")
}
