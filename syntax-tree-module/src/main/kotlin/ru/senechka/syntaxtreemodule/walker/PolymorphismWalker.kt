package ru.senechka.syntaxtreemodule.walker
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Lexer
import grammar.Java20Parser
import grammar.Java20ParserBaseListener

class PolymorphismWalker : Java20ParserBaseListener() {

    data class PolymorphismInfo(
        val className: String,
        val isAbstract: Boolean,
        val abstractMethods: List<MethodInfo>,
        val overriddenMethods: List<MethodInfo>,
        val polymorphicMethods: List<MethodInfo>
    )

    data class MethodInfo(
        val name: String,
        val returnType: String,
        val parameters: List<String>,
        val isAbstract: Boolean,
        val isOverride: Boolean,
        val lineNumber: Int
    )

    private val classInfoMap = mutableMapOf<String, PolymorphismInfo>()
    private var currentClass: String? = null
    private var currentClassIsAbstract = false
    private val currentMethods = mutableListOf<MethodInfo>()
    private val inheritanceGraph = mutableMapOf<String, String?>()

    fun getPolymorphismInfo(): Map<String, PolymorphismInfo> = classInfoMap.toMap()

    override fun enterNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        currentClass = ctx.typeIdentifier().text
        currentClassIsAbstract = ctx.classModifier().any { it.text == "abstract" }
        inheritanceGraph[currentClass!!] = ctx.classExtends()?.classType()?.text
        currentMethods.clear()
    }

    override fun exitNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        val classInfo = PolymorphismInfo(
            className = currentClass!!,
            isAbstract = currentClassIsAbstract,
            abstractMethods = currentMethods.filter { it.isAbstract },
            overriddenMethods = currentMethods.filter { it.isOverride },
            polymorphicMethods = findPolymorphicMethods()
        )
        classInfoMap[currentClass!!] = classInfo
        currentClass = null
    }

    override fun enterMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        val methodName = ctx.methodHeader().methodDeclarator().identifier().text
        val returnType = ctx.methodHeader().result().text
        val parameters = ctx.methodHeader().methodDeclarator().formalParameterList()
            ?.formalParameter()
            ?.map { it.unannType().text } ?: emptyList()

        val isAbstract = ctx.methodModifier().any { it.text == "abstract" } || ctx.methodBody().isEmpty
        val isOverride = ctx.methodModifier().any {
            it.annotation()?.normalAnnotation()?.typeName()?.text == "Override"
        }

        currentMethods.add(
            MethodInfo(
                name = methodName,
                returnType = returnType,
                parameters = parameters,
                isAbstract = isAbstract,
                isOverride = isOverride,
                lineNumber = ctx.start.line
            )
        )
    }

    private fun findPolymorphicMethods(): List<MethodInfo> {
        val polymorphicMethods = mutableListOf<MethodInfo>()
        val superClass = inheritanceGraph[currentClass] ?: return emptyList()

        classInfoMap[superClass]?.polymorphicMethods?.forEach { superMethod ->
            currentMethods.firstOrNull {
                it.name == superMethod.name &&
                        it.parameters == superMethod.parameters
            }?.let {
                polymorphicMethods.add(it)
            }
        }

        return polymorphicMethods
    }

    override fun enterNormalInterfaceDeclaration(ctx: Java20Parser.NormalInterfaceDeclarationContext) {
        currentClass = ctx.typeIdentifier().text
        currentClassIsAbstract = true
        currentMethods.clear()
    }

    override fun exitNormalInterfaceDeclaration(ctx: Java20Parser.NormalInterfaceDeclarationContext) {
        val classInfo = PolymorphismInfo(
            className = currentClass!!,
            isAbstract = true,
            abstractMethods = currentMethods,
            overriddenMethods = emptyList(),
            polymorphicMethods = emptyList()
        )
        classInfoMap[currentClass!!] = classInfo
        currentClass = null
    }

    companion object {
        fun analyze(tokens: CommonTokenStream): PolymorphismWalker {
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = PolymorphismWalker()
            walker.walk(listener, tree)

            return listener
        }
    }
}