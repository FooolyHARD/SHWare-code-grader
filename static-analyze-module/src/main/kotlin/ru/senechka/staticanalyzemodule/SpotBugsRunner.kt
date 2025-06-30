package ru.senechka.staticmodule.runner

import edu.umd.cs.findbugs.*
import edu.umd.cs.findbugs.config.UserPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object SpotBugsRunner {

    @Serializable
    data class SpotBugsViolation(
        val className: String,
        val lineNumber: Int,
        val type: String,
        val message: String
    )

    @Serializable
    data class SpotBugsAnalysisResult(
        val analysis: String = "SpotBugs",
        val violations: List<SpotBugsViolation>
    )

    fun analyze(classDir: File): String {
        val project = Project().apply {
            addFile(classDir.absolutePath)
        }

        val bugReporter = BugCollectionBugReporter(project).apply {
            setPriorityThreshold(Priorities.NORMAL_PRIORITY)
            setErrorVerbosity(0)
        }
        val findBugs = FindBugs2().apply {
            setProject(project)
            userPreferences = UserPreferences.createDefaultUserPreferences()
            setBugReporter(bugReporter)
            setDetectorFactoryCollection(DetectorFactoryCollection())
        }

        try {
            findBugs.execute()
        } catch (e: Exception) {
            println("[SpotBugs] Ошибка выполнения: ${e.message}")
            return Json.encodeToString(SpotBugsAnalysisResult(violations = emptyList()))
        }

        val bugCollection = bugReporter.bugCollection

        val violations = bugCollection.collection.map { bug ->
            SpotBugsViolation(
                className = bug.primaryClass?.className ?: "<unknown>",
                lineNumber = bug.primarySourceLineAnnotation?.startLine ?: -1,
                type = bug.type,
                message = bug.message
            )
        }

        return Json.encodeToString(SpotBugsAnalysisResult(violations = violations))
    }
}
