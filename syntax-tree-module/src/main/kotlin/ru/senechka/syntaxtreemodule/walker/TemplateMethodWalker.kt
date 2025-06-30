package ru.senechka.syntaxtreemodule.walker

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Parser
import grammar.Java20Lexer
import grammar.Java20ParserBaseListener


class TemplateMethodWalker : Java20ParserBaseListener() {

    data class TemplateMethodInfo(
        val className: String,
        val templateMethods: List<MethodInfo>,
        val hookMethods: List<MethodInfo>
    )

    data class MethodInfo(
        val name: String,
        val isFinal: Boolean,
        val calls: List<String>,
        val line: Int
    )

    private val results = mutableMapOf<String, TemplateMethodInfo>()
    private var currentClass: String? = null
    private var isAbstractClass = false
    private val abstractMethods = mutableSetOf<String>()
    private val templateMethods = mutableListOf<MethodInfo>()
    private val hookMethods = mutableListOf<MethodInfo>()
    private val methodStack = mutableListOf<String>()

    fun getResults(): Map<String, TemplateMethodInfo> = results.toMap()

    override fun enterClassDeclaration(ctx: Java20Parser.ClassDeclarationContext) {
        currentClass = ctx.normalClassDeclaration().typeIdentifier().text
        isAbstractClass = ctx.normalClassDeclaration().classModifier().any { it.text == "abstract" }
        abstractMethods.clear()
        templateMethods.clear()
        hookMethods.clear()
    }

    override fun exitClassDeclaration(ctx: Java20Parser.ClassDeclarationContext) {
        if (isAbstractClass && templateMethods.isNotEmpty()) {
            results[currentClass!!] = TemplateMethodInfo(
                className = currentClass!!,
                templateMethods = templateMethods.toList(),
                hookMethods = hookMethods.toList()
            )
        }
        currentClass = null
    }

    override fun enterMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        val name = ctx.methodHeader().methodDeclarator().identifier().text
        val isAbstract = ctx.methodModifier().any { it.text == "abstract" } ||
                ctx.methodBody()?.isEmpty() == true

        if (isAbstract) {
            abstractMethods.add(name)
            return
        }

        methodStack.add(name)
    }

    override fun exitMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        if (methodStack.isNotEmpty()) {
            methodStack.removeLast()
        }
    }

    override fun enterMethodInvocation(ctx: Java20Parser.MethodInvocationContext) {
        val methodName = ctx.methodName().text
        val currentMethod = methodStack.lastOrNull() ?: return

        if (abstractMethods.contains(methodName)) {
            val existing = templateMethods.find { it.name == currentMethod }
            if (existing != null) {
                if (methodName !in existing.calls) {
                    templateMethods.replaceAll {
                        if (it.name == currentMethod) it.copy(calls = it.calls + methodName) else it
                    }
                }
            } else {
                templateMethods.add(
                    MethodInfo(
                        name = currentMethod,
                        isFinal = ctx.parent.parent is Java20Parser.MethodDeclarationContext &&
                                (ctx.parent.parent as Java20Parser.MethodDeclarationContext)
                                    .methodModifier().any { it.text == "final" },
                        calls = listOf(methodName),
                        line = ctx.start.line
                    )
                )
            }
        }
    }

    override fun enterMethodBody(ctx: Java20Parser.MethodBodyContext) {
        if (isAbstractClass && ctx.isEmpty && methodStack.isNotEmpty()) {
            hookMethods.add(
                MethodInfo(
                    name = methodStack.last(),
                    isFinal = false,
                    calls = emptyList(),
                    line = ctx.start.line
                )
            )
        }
    }

    companion object {
        fun analyze(tokens: CommonTokenStream): TemplateMethodWalker {
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = TemplateMethodWalker()
            walker.walk(listener, tree)
            return listener
        }
    }
}