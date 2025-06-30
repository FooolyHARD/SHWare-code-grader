package ru.senechka.orchestratormodule

import kotlinx.serialization.json.Json
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import ru.senechka.staticanalyzemodule.StaticAnalyzeLauncher
import java.io.File

@SpringBootApplication
class OrchestratorModuleApplication

fun main(args: Array<String>) {
    val userPrompt = "Найди паттерн синглтон в коде"

    val javaFile = File("/home/arseny/repos/newdex-code-grader-service/syntax-tree-module/source-code/Main.java")

    if (!javaFile.exists()) {
        println("❌ Файл не найден: ${javaFile.absolutePath}")
        return
    }

    val code = javaFile.readText()

    val result = OrchestratorModule.handleUserQuery(code, userPrompt)
    println(result)

    val staticJson = StaticAnalyzeLauncher.run(
        classDir = File("/home/arseny/repos/newdex-code-grader-service/syntax-tree-module/source-code/classes"),
        sourceDir = File("/home/arseny/repos/newdex-code-grader-service/syntax-tree-module/source-code/"),
        checkstyleConfig = File("/home/arseny/repos/newdex-code-grader-service/static-analyze-module/src/main/kotlin/ru/senechka/staticanalyzemodule/config/checkstyle.xml")
    )

    println(staticJson)



}
