package ru.senechka.syntaxtreemodule.walker

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Parser
import grammar.Java20ParserBaseListener

class CompositionWalker : Java20ParserBaseListener() {

    data class ClassInfo(
        val className: String,
        val superClass: String?,
        val fields: List<String>,
        val line: Int
    )

    private var currentClassName: String? = null
    private var currentSuperClass: String? = null
    private val currentFields = mutableListOf<String>()
    private var currentLine = 0

    private val classes = mutableListOf<ClassInfo>()

    fun getClassInfo(): List<ClassInfo> = classes

    override fun enterNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        currentClassName = ctx.typeIdentifier().text
        currentSuperClass = ctx.classExtends()?.classType()?.text
        currentFields.clear()
        currentLine = ctx.start.line
    }

    override fun exitNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        if (currentClassName != null) {
            classes.add(
                ClassInfo(
                    className = currentClassName!!,
                    superClass = currentSuperClass,
                    fields = currentFields.toList(),
                    line = currentLine
                )
            )
        }
        currentClassName = null
        currentSuperClass = null
    }

    override fun enterFieldDeclaration(ctx: Java20Parser.FieldDeclarationContext) {
        val fieldType = ctx.unannType()?.text ?: return
        currentFields.add(fieldType)
    }

    companion object {
        fun analyze(tokens: CommonTokenStream): CompositionWalker {
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = CompositionWalker()
            walker.walk(listener, tree)

            return listener
        }
    }
}
