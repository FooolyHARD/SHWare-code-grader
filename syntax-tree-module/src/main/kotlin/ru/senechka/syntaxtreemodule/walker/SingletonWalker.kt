package ru.senechka.syntaxtreemodule.walker

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Parser
import grammar.Java20ParserBaseListener

class SingletonWalker : Java20ParserBaseListener() {

    data class SingletonIssue(
        val className: String,
        val issues: List<String>,
        val line: Int
    )

    private var currentClassName: String? = null
    private var currentClassLine: Int = 0
    private var hasPrivateConstructor = false
    private var hasStaticInstance = false
    private var hasGetInstanceMethod = false
    private var getInstanceCount = 0
    private var usesNewInsideClass = false

    private val violations = mutableListOf<SingletonIssue>()

    fun getViolations(): List<SingletonIssue> = violations

    override fun enterNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        currentClassName = ctx.typeIdentifier().text
        currentClassLine = ctx.start.line
        hasPrivateConstructor = false
        hasStaticInstance = false
        hasGetInstanceMethod = false
        getInstanceCount = 0
        usesNewInsideClass = false
    }

    override fun exitNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        val issues = mutableListOf<String>()

        if (!hasPrivateConstructor) issues.add("Отсутствует приватный конструктор")
        if (!hasStaticInstance) issues.add("Отсутствует статическое поле instance")
        if (!hasGetInstanceMethod) issues.add("Отсутствует метод getInstance()")
        if (getInstanceCount > 1) issues.add("Несколько методов getInstance()")
        if (usesNewInsideClass) issues.add("Класс создает экземпляры сам через new")

        if (issues.isNotEmpty()) {
            violations.add(
                SingletonIssue(
                    className = currentClassName ?: "<unknown>",
                    issues = issues,
                    line = currentClassLine
                )
            )
        }

        currentClassName = null
    }

    override fun enterConstructorDeclaration(ctx: Java20Parser.ConstructorDeclarationContext) {
        val isPrivate = ctx.constructorModifier().any { it.text == "private" }
        if (isPrivate) {
            hasPrivateConstructor = true
        }
    }

    override fun enterFieldDeclaration(ctx: Java20Parser.FieldDeclarationContext) {
        val isStatic = ctx.fieldModifier().any { it.text == "static" }
        val isInstanceNamed = ctx.variableDeclaratorList()?.text?.contains("instance") == true
        if (isStatic && isInstanceNamed) {
            hasStaticInstance = true
        }
    }

    override fun enterMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        val methodName = ctx.methodHeader().methodDeclarator().identifier().text
        if (methodName.equals("getInstance", ignoreCase = true)) {
            hasGetInstanceMethod = true
            getInstanceCount++
        }
    }

    override fun enterExpression(ctx: Java20Parser.ExpressionContext) {
        // если класс сам делает new (возможно, getInstance не используется корректно)
        if (ctx.text.contains("new ")) {
            usesNewInsideClass = true
        }
    }

    companion object {
        fun analyze(tokens: CommonTokenStream): SingletonWalker {
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = SingletonWalker()
            walker.walk(listener, tree)

            return listener
        }
    }
}
