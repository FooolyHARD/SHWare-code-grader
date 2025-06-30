package ru.senechka.syntaxtreemodule.walker

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Parser
import grammar.Java20ParserBaseListener

class AccessModifierWalker : Java20ParserBaseListener() {

    data class FieldViolation(
        val className: String,
        val fieldName: String,
        val modifier: String,
        val lineNumber: Int
    )

    private var currentClassName: String? = null
    private val violations = mutableListOf<FieldViolation>()

    fun getViolations(): List<FieldViolation> = violations

    override fun enterNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        currentClassName = ctx.typeIdentifier().text
    }

    override fun enterFieldDeclaration(ctx: Java20Parser.FieldDeclarationContext) {
        val modifiers = ctx.fieldModifier().mapNotNull { it.text }
        val modifier = when {
            "public" in modifiers -> "public"
            "protected" in modifiers -> "protected"
            "private" in modifiers -> "private"
            else -> "package-private"
        }

        // Если не private — потенциальное нарушение
        if (modifier != "private") {
            val fields = ctx.variableDeclaratorList().variableDeclarator()
            fields.forEach { declarator ->
                val fieldName = declarator.variableDeclaratorId().text
                violations.add(
                    FieldViolation(
                        className = currentClassName ?: "<unknown>",
                        fieldName = fieldName,
                        modifier = modifier,
                        lineNumber = ctx.start.line
                    )
                )
            }
        }
    }

    companion object {
        fun analyze(tokens: CommonTokenStream): AccessModifierWalker {
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = AccessModifierWalker()
            walker.walk(listener, tree)

            return listener
        }
    }
}
