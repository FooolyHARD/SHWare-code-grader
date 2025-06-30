import grammar.Java20Parser
import grammar.Java20ParserBaseListener
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.ParseTreeWalker

import java.io.File

class LoopWalker(private val tokens: CommonTokenStream) {

    data class LoopInfo(
        val lineNumber: Int,
        val loopType: String,
        val codeSnippet: String
    )

    fun analyze(): List<LoopInfo> {
        val parser = Java20Parser(tokens)
        val tree = parser.compilationUnit()
        val listener = LoopListener(tokens)

        ParseTreeWalker.DEFAULT.walk(listener, tree)
        return listener.foundLoops
    }

    private class LoopListener(private val tokens: CommonTokenStream) : Java20ParserBaseListener() {
        val foundLoops = mutableListOf<LoopInfo>()

        private fun getCodeSnippet(ctx: ParserRuleContext): String {
            val start = ctx.start.startIndex
            val stop = ctx.stop?.stopIndex ?: start
            return tokens.getText(Interval.of(start, stop)).trim().take(50) + "..."
        }

        override fun enterForStatement(ctx: Java20Parser.ForStatementContext) {
            foundLoops.add(LoopInfo(
                lineNumber = ctx.start.line,
                loopType = "for",
                codeSnippet = getCodeSnippet(ctx)
            ))
        }

        override fun enterWhileStatement(ctx: Java20Parser.WhileStatementContext) {
            foundLoops.add(LoopInfo(
                lineNumber = ctx.start.line,
                loopType = "while",
                codeSnippet = getCodeSnippet(ctx)
            ))
        }

        override fun enterDoStatement(ctx: Java20Parser.DoStatementContext) {
            foundLoops.add(LoopInfo(
                lineNumber = ctx.start.line,
                loopType = "do-while",
                codeSnippet = getCodeSnippet(ctx)
            ))
        }

        override fun enterEnhancedForStatement(ctx: Java20Parser.EnhancedForStatementContext) {
            foundLoops.add(LoopInfo(
                lineNumber = ctx.start.line,
                loopType = "for-each",
                codeSnippet = getCodeSnippet(ctx)
            ))
        }
    }
}