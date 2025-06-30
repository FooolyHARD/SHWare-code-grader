package ru.senechka.staticanalyzemodule

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.senechka.staticmodule.runner.SpotBugsRunner

import java.io.File

object StaticAnalyzeLauncher {
    fun run(classDir: File, sourceDir: File, checkstyleConfig: File): String {
        val spot = SpotBugsRunner.analyze(classDir)
        val style = if (checkstyleConfig.exists()) {
            CheckstyleRunner.analyze(sourceDir, checkstyleConfig)
        } else null

        return Json.encodeToString(
            StaticAnalysisResult(spotbugs = spot, checkstyle = style)
        )
    }
}