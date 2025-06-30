package ru.senechka.syntaxtreemodule.walker

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Parser
import grammar.Java20ParserBaseListener

class InterfaceSegregationWalker : Java20ParserBaseListener() {

    data class InterfaceViolation(
        val interfaceName: String,
        val methodCount: Int,
        val lineNumber: Int
    )

    private val violationsMap = mutableMapOf<String, InterfaceViolation>()
    private var currentInterface: String? = null
    private var methodCount = 0
    private var interfaceLineNumber: Int = 0
    private var insideInterface = false

    fun getInterfaceViolations(): Map<String, InterfaceViolation> = violationsMap.toMap()

    override fun enterNormalInterfaceDeclaration(ctx: Java20Parser.NormalInterfaceDeclarationContext) {
        currentInterface = ctx.typeIdentifier().text
        interfaceLineNumber = ctx.start.line
        methodCount = 0
        insideInterface = true
    }

    override fun exitNormalInterfaceDeclaration(ctx: Java20Parser.NormalInterfaceDeclarationContext) {
        if (insideInterface && currentInterface != null && methodCount > 3) {
            violationsMap[currentInterface!!] = InterfaceViolation(
                interfaceName = currentInterface!!,
                methodCount = methodCount,
                lineNumber = interfaceLineNumber
            )
        }
        currentInterface = null
        insideInterface = false
    }

    override fun enterInterfaceMemberDeclaration(ctx: Java20Parser.InterfaceMemberDeclarationContext) {
        if (!insideInterface) return

        // Проверка: является ли член интерфейса методом
        val interfaceMethodDecl = ctx.interfaceMethodDeclaration()
        if (interfaceMethodDecl != null) {
            methodCount++
        }
    }

    companion object {
        fun analyze(tokens: CommonTokenStream): InterfaceSegregationWalker {
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = InterfaceSegregationWalker()
            walker.walk(listener, tree)

            return listener
        }
    }
}
