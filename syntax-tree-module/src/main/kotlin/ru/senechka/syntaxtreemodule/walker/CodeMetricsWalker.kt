package ru.senechka.syntaxtreemodule.walker

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Parser
import grammar.Java20ParserBaseListener

class CodeMetricsWalker : Java20ParserBaseListener() {

    data class MethodMetrics(
        val methodName: String,
        val lineNumber: Int,
        val length: Int,
        val cyclomaticComplexity: Int
    )

    private val metricsList = mutableListOf<MethodMetrics>()

    private var currentMethodName: String? = null
    private var currentMethodStartLine: Int = 0
    private var currentMethodEndLine: Int = 0
    private var currentCyclomaticComplexity: Int = 1 // Начинаем с 1
    private var insideMethod = false

    fun getMetrics(): List<MethodMetrics> = metricsList.toList()

    override fun enterMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        currentMethodName = ctx.methodHeader()?.methodDeclarator()?.identifier()?.text ?: "<anonymous>"
        currentMethodStartLine = ctx.start.line
        currentCyclomaticComplexity = 1
        insideMethod = true
    }

    override fun exitMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        if (insideMethod) {
            currentMethodEndLine = ctx.stop.line
            val length = currentMethodEndLine - currentMethodStartLine + 1

            metricsList.add(
                MethodMetrics(
                    methodName = currentMethodName ?: "<anonymous>",
                    lineNumber = currentMethodStartLine,
                    length = length,
                    cyclomaticComplexity = currentCyclomaticComplexity
                )
            )
        }
        insideMethod = false
        currentMethodName = null
    }

    // Управляющие конструкции, влияющие на цикломатическую сложность
    override fun enterIfThenStatement(ctx: Java20Parser.IfThenStatementContext) {
        if (insideMethod) currentCyclomaticComplexity++
    }

    override fun enterIfThenElseStatement(ctx: Java20Parser.IfThenElseStatementContext) {
        if (insideMethod) currentCyclomaticComplexity++
    }

    override fun enterWhileStatement(ctx: Java20Parser.WhileStatementContext) {
        if (insideMethod) currentCyclomaticComplexity++
    }

    override fun enterDoStatement(ctx: Java20Parser.DoStatementContext) {
        if (insideMethod) currentCyclomaticComplexity++
    }

    override fun enterForStatement(ctx: Java20Parser.ForStatementContext) {
        if (insideMethod) currentCyclomaticComplexity++
    }

    override fun enterEnhancedForStatement(ctx: Java20Parser.EnhancedForStatementContext) {
        if (insideMethod) currentCyclomaticComplexity++
    }

    override fun enterSwitchLabel(ctx: Java20Parser.SwitchLabelContext) {
        if (insideMethod && ctx.CASE() != null) {
            currentCyclomaticComplexity++
        }
    }

    override fun enterCatchClause(ctx: Java20Parser.CatchClauseContext) {
        if (insideMethod) currentCyclomaticComplexity++
    }

    override fun enterConditionalExpression(ctx: Java20Parser.ConditionalExpressionContext) {
        if (insideMethod && ctx.QUESTION() != null) {
            currentCyclomaticComplexity++
        }
    }

    override fun enterConditionalAndExpression(ctx: Java20Parser.ConditionalAndExpressionContext) {
        if (insideMethod && ctx.AND() != null) {
            currentCyclomaticComplexity++
        }
    }

    override fun enterConditionalOrExpression(ctx: Java20Parser.ConditionalOrExpressionContext) {
        if (insideMethod && ctx.OR() != null) {
            currentCyclomaticComplexity++
        }
    }

    companion object {
        fun analyze(tokens: CommonTokenStream): CodeMetricsWalker {
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = CodeMetricsWalker()
            walker.walk(listener, tree)

            return listener
        }
    }
}
