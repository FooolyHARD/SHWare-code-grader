package ru.senechka.syntaxtreemodule.walker

import grammar.Java20Parser
import grammar.Java20ParserBaseListener
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*


class SolidWalker(private val tokens: CommonTokenStream) {

    data class SolidViolation(
        val principle: String,
        val className: String?,
        val lineNumber: Int,
        val message: String
    )

    fun analyze(): List<SolidViolation> {
        val parser = Java20Parser(tokens)
        val tree = parser.compilationUnit()
        val listener = SolidAnalysisListener(tokens)

        ParseTreeWalker.DEFAULT.walk(listener, tree)
        return listener.violations
    }

    private class SolidAnalysisListener(private val tokens: CommonTokenStream) : Java20ParserBaseListener() {
        val violations = mutableListOf<SolidViolation>()
        private var currentClass: String? = null
        private val methodCounts = mutableMapOf<String, Int>()
        private val dependencies = mutableMapOf<String, MutableSet<String>>()

        override fun enterNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
            currentClass = ctx.typeIdentifier().text
            methodCounts[currentClass!!] = 0

            ctx.classImplements()?.let {
                violations.add(
                    SolidViolation(
                        "DIP",
                        currentClass,
                        ctx.start.line,
                        "Implements ${it.interfaceTypeList().text}"
                    )
                )
            }
        }

        override fun exitNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
            checkSrpViolation(ctx.start.line)
            currentClass = null
        }

        private fun checkSrpViolation(line: Int) {
            currentClass?.let { className ->
                methodCounts[className]?.let { count ->
                    if (count > 10) {
                        violations.add(
                            SolidViolation(
                                "SRP",
                                className,
                                line,
                                "Class has $count methods (should be <= 10)"
                            )
                        )
                    }
                }
            }
        }

        override fun enterClassBodyDeclaration(ctx: Java20Parser.ClassBodyDeclarationContext) {
            when {
                ctx.classMemberDeclaration()?.methodDeclaration() != null -> {
                    currentClass?.let { methodCounts[it] = methodCounts[it]!! + 1 }
                }
                ctx.classMemberDeclaration()?.fieldDeclaration() != null -> {
                    analyzeFieldDeclaration(ctx.classMemberDeclaration()!!.fieldDeclaration()!!)
                }
            }
        }

        private fun analyzeFieldDeclaration(ctx: Java20Parser.FieldDeclarationContext) {
            val fieldType = ctx.unannType().text
            currentClass?.let {
                dependencies.getOrPut(it) { mutableSetOf() }.add(fieldType)
                if (!fieldType.contains("Interface") && !fieldType.contains("Abstract")) {
                    violations.add(
                        SolidViolation(
                            "DIP",
                            currentClass,
                            ctx.start.line,
                            "Depends on concrete type: $fieldType"
                        )
                    )
                }
            }
        }

        override fun enterMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
            if (ctx.methodBody().isEmpty && ctx.methodModifier().any {
                    it.annotation()?.normalAnnotation()?.typeName()?.text == "Override"
                }) {
                violations.add(
                    SolidViolation(
                        "LSP",
                        currentClass,
                        ctx.start.line,
                        "Empty @Override method violates Liskov principle"
                    )
                )
            }
        }
//list fnc -> use case -> time ->
        override fun enterSwitchExpression(ctx: Java20Parser.SwitchExpressionContext) {
            val caseCount = ctx.switchBlock().children.count {
                it is Java20Parser.SwitchRuleContext
            }
            if (caseCount > 3) {
                violations.add(
                    SolidViolation(
                        "OCP",
                        currentClass,
                        ctx.start.line,
                        "Switch expression with $caseCount cases suggests need for polymorphism"
                    )
                )
            }
        }

        override fun enterNormalInterfaceDeclaration(ctx: Java20Parser.NormalInterfaceDeclarationContext) {
            val methods = ctx.interfaceBody().interfaceMemberDeclaration()
                .count { it.interfaceMethodDeclaration() != null }

            if (methods > 5) {
                violations.add(
                    SolidViolation(
                        "ISP",
                        ctx.typeIdentifier().text,
                        ctx.start.line,
                        "Interface has $methods methods (should be <= 5)"
                    )
                )
            }
        }
    }

    companion object {
        fun printReport(violations: List<SolidViolation>) {
            if (violations.isEmpty()) {
                println("No SOLID violations found")
                return
            }

            val grouped = violations.groupBy { it.principle }
            println("SOLID ANALYSIS REPORT")
            println("=".repeat(50))

            grouped.forEach { (principle, violations) ->
                println("\n$principle Violations (${violations.size}):")
                violations.forEach {
                    println(" - ${it.className ?: "Global"} [Line ${it.lineNumber}]: ${it.message}")
                }
            }
        }
    }
}