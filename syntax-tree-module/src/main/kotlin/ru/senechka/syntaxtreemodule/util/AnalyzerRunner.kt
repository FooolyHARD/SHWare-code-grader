package ru.senechka.syntaxtreemodule.util

import LoopWalker
import grammar.Java20Lexer
import kotlinx.serialization.encodeToString
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import ru.senechka.syntaxtreemodule.util.Reporter.ViolationResult
import ru.senechka.syntaxtreemodule.walker.*

object AnalyzerRunner {

    fun analyze(code: String, workers: List<String>): String {
        val lexer = Java20Lexer(CharStreams.fromString(code))
        val tokens = CommonTokenStream(lexer)

        val results = mutableListOf<Reporter.AnalysisResult>()

        for (worker in workers) {
            when (worker.lowercase()) {
                "singleton" -> {
                    val violations = SingletonWalker.analyze(tokens).getViolations().map {
                        ViolationResult(it.className, it.line, it.issues)
                    }
                    results.add(Reporter.AnalysisResult("Singleton", violations))
                }

                "isp", "interfacesegregation" -> {
                    val violations = InterfaceSegregationWalker.analyze(tokens).getInterfaceViolations().values.map {
                        ViolationResult(it.interfaceName, it.lineNumber, listOf("Слишком много методов: ${it.methodCount}"))
                    }
                    results.add(Reporter.AnalysisResult("Interface Segregation Principle", violations))
                }

                "access", "encapsulation" -> {
                    val violations = AccessModifierWalker.analyze(tokens).getViolations().map {
                        ViolationResult(it.className, it.lineNumber, listOf("${it.fieldName} — ${it.modifier}"))
                    }
                    results.add(Reporter.AnalysisResult("Инкапсуляция", violations))
                }

                "factory" -> {
                    val violations = FactoryPatternWalker.analyze(tokens).getFactoryContainers().map {
                        val messages = it.methods.map { m ->
                            "${m.methodName}(${m.parameters}) → ${m.returnType}"
                        }
                        ViolationResult(it.name, 0, messages)
                    }
                    results.add(Reporter.AnalysisResult("Factory Pattern", violations))
                }

                "composition" -> {
                    val violations = CompositionWalker.analyze(tokens).getClassInfo().map {
                        val verdict = when {
                            it.fields.isNotEmpty() && it.superClass == null -> "✅ Композиция без наследования"
                            it.fields.isNotEmpty() -> "🟡 Композиция + наследование"
                            it.superClass != null -> "❌ Только наследование"
                            else -> "💤 Пустой класс"
                        }
                        ViolationResult(it.className, it.line, listOf("Наследует: ${it.superClass ?: "—"}", "Поля: ${it.fields.joinToString()}", verdict))
                    }
                    results.add(Reporter.AnalysisResult("Composition vs Inheritance", violations))
                }

                "metrics", "codemetrics" -> {
                    val violations = CodeMetricsWalker.analyze(tokens).getMetrics().map {
                        ViolationResult(
                            it.methodName,
                            it.lineNumber,
                            listOf("Длина: ${it.length}", "Сложность: ${it.cyclomaticComplexity}")
                        )
                    }
                    results.add(Reporter.AnalysisResult("Метрики кода", violations))
                }

                "loops" -> {
                    val violations = LoopWalker(tokens).analyze().map {
                        ViolationResult("${it.loopType} loop", it.lineNumber, listOf(it.codeSnippet))
                    }
                    results.add(Reporter.AnalysisResult("Циклы", violations))
                }

                "solid" -> {
                    val solidWalker = SolidWalker(tokens)
                    val analysis = solidWalker.analyze()
                    val violations = analysis.map {
                        ViolationResult(
                            it.className ?: "<unknown>",
                            it.lineNumber,
                            listOf("${it.principle}: ${it.message}")
                        )
                    }
                    results.add(Reporter.AnalysisResult("SOLID", violations))
                }

                "inheritance" -> {
                    val inheritanceWalker = InheritanceWalker.analyze(tokens)
                    val graph = inheritanceWalker.getInheritanceGraph().map { (name, info) ->
                        ViolationResult(name, 0, listOf("Extends: ${info.superClass ?: "Object"}", "Implements: ${info.interfaces.joinToString()}"))
                    }
                    val deep = inheritanceWalker.findDeepestHierarchy()?.let {
                        ViolationResult(it.first, 0, listOf("Самая глубокая иерархия: ${it.second}"))
                    }
                    val multiple = inheritanceWalker.findMultipleInheritance().map {
                        ViolationResult(it.key, 0, listOf("Множественное наследование интерфейсов: ${it.value.joinToString()}"))
                    }
                    results.add(Reporter.AnalysisResult("Наследование", graph + multiple + listOfNotNull(deep)))
                }

                "polymorphism" -> {
                    val polyWalker = PolymorphismWalker.analyze(tokens).getPolymorphismInfo()
                    val violations = polyWalker.map { (className, info) ->
                        val details = mutableListOf<String>()
                        if (info.abstractMethods.isNotEmpty()) details += "Абстрактные методы: ${info.abstractMethods.joinToString { it.name }}"
                        if (info.overriddenMethods.isNotEmpty()) details += "Переопределения: ${info.overriddenMethods.joinToString { it.name }}"
                        if (info.polymorphicMethods.isNotEmpty()) details += "Полиморфные методы: ${info.polymorphicMethods.joinToString { it.name }}"
                        ViolationResult(className, 0, details)
                    }
                    results.add(Reporter.AnalysisResult("Полиморфизм", violations))
                }

                "template" -> {
                    val tmpl = TemplateMethodWalker.analyze(tokens).getResults()
                    val violations = tmpl.map { (className, result) ->
                        val details = mutableListOf<String>()
                        details += result.templateMethods.map { "Шаблон: ${it.name} → вызывает: ${it.calls.joinToString()}" }
                        details += result.hookMethods.map { "Hook: ${it.name}" }
                        ViolationResult(className, 0, details)
                    }
                    results.add(Reporter.AnalysisResult("Template Method", violations))
                }

                "dip", "dependency" -> {
                    val walker = DependencyWalker.analyze(tokens)
                    val violations = walker.getDependencyViolations().flatMap { (className, v) ->
                        val deps = v.concreteDependencies.map {
                            "Concrete ${it.dependencyType} в ${it.usedIn} (строка ${it.lineNumber})"
                        }
                        val dips = v.dipViolations.map {
                            "${it.highLevelModule} зависит от ${it.lowLevelModule} — ${it.dependencyType} (строка ${it.lineNumber})"
                        }
                        listOf(ViolationResult(className, 0, deps + dips))
                    }
                    results.add(Reporter.AnalysisResult("Dependency Inversion", violations))
                }
            }
        }

        return kotlinx.serialization.json.Json.encodeToString<List<Reporter.AnalysisResult>>(results)
    }
}
