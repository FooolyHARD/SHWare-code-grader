package ru.senechka.syntaxtreemodule.walker

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Lexer
import grammar.Java20Parser
import grammar.Java20ParserBaseListener

class InheritanceWalker : Java20ParserBaseListener() {

    data class InheritanceInfo(
        val className: String,
        val superClass: String?,
        val interfaces: List<String>,
        val depth: Int = 0,
        val lineNumber: Int
    )

    private val inheritanceGraph = mutableMapOf<String, InheritanceInfo>()
    private var currentClass: String? = null
    private val classStack = mutableListOf<String>()
    private val interfaceImplementations = mutableMapOf<String, MutableList<String>>()

    fun getInheritanceGraph(): Map<String, InheritanceInfo> = inheritanceGraph.toMap()
    fun getInterfaceImplementations(): Map<String, List<String>> = interfaceImplementations.mapValues { it.value.toList() }

    override fun enterNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        val className = ctx.typeIdentifier().text
        currentClass = className
        classStack.add(className)

        val superClass = ctx.classExtends()?.let {
            it.classType().children.joinToString("") { node -> node.text }
        }

        val interfaces = ctx.classImplements()?.interfaceTypeList()?.interfaceType()
            ?.map { it.classType().text } ?: emptyList()

        inheritanceGraph[className] = InheritanceInfo(
            className = className,
            superClass = superClass,
            interfaces = interfaces,
            depth = calculateDepth(superClass),
            lineNumber = ctx.start.line
        )

        interfaces.forEach { interfaceName ->
            interfaceImplementations.getOrPut(interfaceName) { mutableListOf() }.add(className)
        }
    }

    override fun exitNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        classStack.removeLast()
        currentClass = classStack.lastOrNull()
    }

    override fun enterNormalInterfaceDeclaration(ctx: Java20Parser.NormalInterfaceDeclarationContext) {
        val interfaceName = ctx.typeIdentifier().text
        currentClass = interfaceName
        classStack.add(interfaceName)

        val superInterfaces = ctx.interfaceExtends()?.interfaceTypeList()?.interfaceType()
            ?.map { it.classType().text } ?: emptyList()

        inheritanceGraph[interfaceName] = InheritanceInfo(
            className = interfaceName,
            superClass = null,
            interfaces = superInterfaces,
            depth = superInterfaces.maxOfOrNull { calculateDepth(it) } ?: 0,
            lineNumber = ctx.start.line
        )

        superInterfaces.forEach { superInterface ->
            interfaceImplementations.getOrPut(superInterface) { mutableListOf() }.add(interfaceName)
        }
    }

    override fun exitNormalInterfaceDeclaration(ctx: Java20Parser.NormalInterfaceDeclarationContext) {
        classStack.removeLast()
        currentClass = classStack.lastOrNull()
    }

    private fun calculateDepth(superType: String?): Int {
        if (superType == null) return 0
        return inheritanceGraph[superType]?.depth?.plus(1) ?: 1
    }

    fun findMultipleInheritance(): Map<String, List<String>> {
        return inheritanceGraph.filter { it.value.interfaces.size > 1 }
            .mapValues { it.value.interfaces }
    }

    fun findDeepestHierarchy(): Pair<String, Int>? {
        return inheritanceGraph.maxByOrNull { it.value.depth }?.let {
            it.key to it.value.depth
        }
    }

    companion object {
        fun analyze(tokens: CommonTokenStream): InheritanceWalker{
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = InheritanceWalker()
            walker.walk(listener, tree)

            return listener
        }
    }
}