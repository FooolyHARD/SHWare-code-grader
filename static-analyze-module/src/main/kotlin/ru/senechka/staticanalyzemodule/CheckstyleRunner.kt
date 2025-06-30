package ru.senechka.staticanalyzemodule

import com.puppycrawl.tools.checkstyle.api.AuditEvent
import com.puppycrawl.tools.checkstyle.api.AuditListener
import com.puppycrawl.tools.checkstyle.api.CheckstyleException
import com.puppycrawl.tools.checkstyle.Checker
import com.puppycrawl.tools.checkstyle.ConfigurationLoader
import com.puppycrawl.tools.checkstyle.PropertiesExpander
import com.puppycrawl.tools.checkstyle.api.Configuration
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

object CheckstyleRunner {

    @Serializable
    data class CheckstyleViolation(
        val file: String,
        val line: Int,
        val message: String,
        val severity: String,
        val source: String
    )

    @Serializable
    data class CheckstyleAnalysisResult(
        val analysis: String = "Checkstyle",
        val violations: List<CheckstyleViolation>
    )

    fun analyze(sourceDir: File, configFile: File): String {
        val violations = Collections.synchronizedList(mutableListOf<CheckstyleViolation>())

        val listener = object : AuditListener {
            override fun auditStarted(event: AuditEvent?) {}
            override fun auditFinished(event: AuditEvent?) {}
            override fun fileStarted(event: AuditEvent?) {}
            override fun fileFinished(event: AuditEvent?) {}

            override fun addError(event: AuditEvent?) {
                if (event != null && event.severityLevel != null) {
                    violations.add(
                        CheckstyleViolation(
                            file = event.fileName,
                            line = event.line,
                            message = event.message,
                            severity = event.severityLevel.name,
                            source = event.sourceName
                        )
                    )
                }
            }

            override fun addException(event: AuditEvent?, throwable: Throwable?) {
                println("[Checkstyle] Исключение в файле ${event?.fileName}: ${throwable?.message}")
            }
        }

        val checker = Checker().apply {
            setModuleClassLoader(Checker::class.java.classLoader)
            addListener(listener)
        }

        try {
            val config: Configuration = ConfigurationLoader.loadConfiguration(
                configFile.absolutePath,
                PropertiesExpander(System.getProperties()),
                ConfigurationLoader.IgnoredModulesOptions.OMIT
            )


            checker.configure(config)

            val javaFiles = sourceDir.walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .toList()

            checker.process(javaFiles)

        } catch (e: CheckstyleException) {
            println("[Checkstyle] Ошибка: ${e.message}")
            return Json.encodeToString(CheckstyleAnalysisResult(violations = emptyList()))
        } finally {
            checker.destroy()
        }

        return Json.encodeToString(CheckstyleAnalysisResult(violations = violations))
    }
}
