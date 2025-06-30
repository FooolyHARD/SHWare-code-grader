package ru.senechka.syntaxtreemodule.walker

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Parser
import grammar.Java20ParserBaseListener

class FactoryPatternWalker : Java20ParserBaseListener() {

    data class FactoryMethod(
        val containerName: String,
        val methodName: String,
        val lineNumber: Int,
        val returnType: String,
        val parameters: String
    )

    data class FactoryContainer(
        val name: String,
        val type: String,
        val methods: List<FactoryMethod>,
        val hasPrivateConstructor: Boolean = false
    )

    private var currentContainerName: String? = null
    private var currentContainerType: String = ""
    private var hasPrivateConstructor = false
    private val currentMethods = mutableListOf<FactoryMethod>()
    private val factories = mutableListOf<FactoryContainer>()

    private var insideMethod = false
    private var createsObject = false
    private var currentMethodCtx: Java20Parser.MethodDeclarationContext? = null

    fun getFactoryContainers(): List<FactoryContainer> = factories

    override fun enterNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        currentContainerName = ctx.typeIdentifier().text
        currentContainerType = "class"
        hasPrivateConstructor = false
        currentMethods.clear()
    }

    override fun exitNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        if (currentMethods.isNotEmpty()) {
            factories.add(
                FactoryContainer(
                    name = currentContainerName ?: "<unknown>",
                    type = currentContainerType,
                    methods = currentMethods.toList(),
                    hasPrivateConstructor = hasPrivateConstructor
                )
            )
        }
    }

    override fun enterNormalInterfaceDeclaration(ctx: Java20Parser.NormalInterfaceDeclarationContext) {
        currentContainerName = ctx.typeIdentifier().text
        currentContainerType = "interface"
        currentMethods.clear()
    }

    override fun exitNormalInterfaceDeclaration(ctx: Java20Parser.NormalInterfaceDeclarationContext) {
        if (currentMethods.isNotEmpty()) {
            factories.add(
                FactoryContainer(
                    name = currentContainerName ?: "<unknown>",
                    type = currentContainerType,
                    methods = currentMethods.toList()
                )
            )
        }
    }

    override fun enterConstructorDeclaration(ctx: Java20Parser.ConstructorDeclarationContext) {
        val isPrivate = ctx.constructorModifier().any { it.text == "private" }
        if (isPrivate) {
            hasPrivateConstructor = true
        }
    }

    override fun enterMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        val methodHeader = ctx.methodHeader()
        val methodName = methodHeader.methodDeclarator().identifier().text
        val returnType = methodHeader.result().text
        val parameters = methodHeader.methodDeclarator().formalParameterList()?.text ?: "()"

        // Костыль: модификаторы находятся в родителе, поэтому парсим текст
        val fullText = ctx.parent.text
        val isStatic = fullText.contains("static")
        val isPublic = fullText.contains("public")
        val looksLikeFactory = methodName.matches(Regex("(?i)(create|get|build|of|from).*"))

        if (!(isStatic && isPublic && looksLikeFactory)) return

        println(">> Метод $methodName выглядит как фабричный")

        insideMethod = true
        createsObject = false
        currentMethodCtx = ctx
    }

    override fun exitMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        if (insideMethod && createsObject) {
            val methodHeader = ctx.methodHeader()
            val methodName = methodHeader.methodDeclarator().identifier().text
            val returnType = methodHeader.result().text
            val parameters = methodHeader.methodDeclarator().formalParameterList()?.text ?: "()"

            currentMethods.add(
                FactoryMethod(
                    containerName = currentContainerName ?: "<unknown>",
                    methodName = methodName,
                    lineNumber = ctx.start.line,
                    returnType = returnType,
                    parameters = parameters
                )
            )
        }

        insideMethod = false
        currentMethodCtx = null
    }

    override fun enterExpression(ctx: Java20Parser.ExpressionContext) {
        if (insideMethod) {
            val text = ctx.text
            if (text.contains("new")) {
                println("   >> Обнаружено создание объекта: $text")
                createsObject = true
            }
        }
    }

    companion object {
        fun analyze(tokens: CommonTokenStream): FactoryPatternWalker {
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = FactoryPatternWalker()
            walker.walk(listener, tree)

            return listener
        }
    }
}
