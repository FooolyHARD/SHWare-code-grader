package ru.senechka.syntaxtreemodule.walker

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Parser
import grammar.Java20Lexer
import grammar.Java20ParserBaseListener

class DependencyWalker : Java20ParserBaseListener() {

    data class DependencyViolation(
        val className: String,
        val concreteDependencies: List<ConcreteDependency>,
        val dipViolations: List<DIPViolation>
    )

    data class ConcreteDependency(
        val dependencyType: String,
        val usedIn: String,
        val lineNumber: Int
    )

    data class DIPViolation(
        val highLevelModule: String,
        val lowLevelModule: String,
        val dependencyType: String,
        val lineNumber: Int
    )

    private val violationsMap = mutableMapOf<String, DependencyViolation>()
    private var currentClass: String? = null
    private val currentConcreteDeps = mutableListOf<ConcreteDependency>()
    private val currentDIPViolations = mutableListOf<DIPViolation>()
    private val importedTypes = mutableSetOf<String>()
    private var currentPackage: String? = null
    private val importedTypeToPackageMap = mutableMapOf<String, String>()

    fun getDependencyViolations(): Map<String, DependencyViolation> = violationsMap.toMap()

    override fun enterImportDeclaration(ctx: Java20Parser.ImportDeclarationContext) {
        val fullImport = ctx.getText().removePrefix("import").removeSuffix(";").trim()
        val shortName = fullImport.substringAfterLast(".")
        importedTypeToPackageMap[shortName] = fullImport.substringBeforeLast(".")
        importedTypes.add(shortName) // <--- ВАЖНО!
    }


    override fun enterPackageDeclaration(ctx: Java20Parser.PackageDeclarationContext) {
        val fullPackage = ctx.getText().removePrefix("package").removeSuffix(";").trim()
        currentPackage = fullPackage
    }

    override fun enterClassDeclaration(ctx: Java20Parser.ClassDeclarationContext) {
        val normal = ctx.normalClassDeclaration()
        val id = normal?.typeIdentifier()?.text

        println("DEBUG class name = $id")
        currentClass = id ?: "UnknownClass"
        currentConcreteDeps.clear()
        currentDIPViolations.clear()
    }


    override fun exitClassDeclaration(ctx: Java20Parser.ClassDeclarationContext) {
        currentClass?.let {
            if (currentConcreteDeps.isNotEmpty() || currentDIPViolations.isNotEmpty()) {
                violationsMap[it] = DependencyViolation(
                    className = it,
                    concreteDependencies = currentConcreteDeps.toList(),
                    dipViolations = currentDIPViolations.toList()
                )
            }
        }
        currentClass = null
    }

    override fun enterFieldDeclaration(ctx: Java20Parser.FieldDeclarationContext) {
        // Пробуем взять первый тип, который начинается с заглавной буквы
        val typeName = ctx.children?.firstOrNull {
            it.text.matches(Regex("[A-Z][a-zA-Z0-9_]*"))
        }?.text ?: return

        checkForConcreteDependency(typeName, "field declaration", ctx.start.line)
    }

    override fun enterLocalVariableDeclaration(ctx: Java20Parser.LocalVariableDeclarationContext) {
        val typeName = ctx.children?.firstOrNull {
            it.text.matches(Regex("[A-Z][a-zA-Z0-9_]*"))
        }?.text ?: return

        checkForConcreteDependency(typeName, "local variable", ctx.start.line)
    }


    override fun enterMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        val returnType = ctx.children?.firstOrNull {
            it.text.matches(Regex("[A-Z][a-zA-Z0-9_]*"))
        }?.text

        if (returnType != null) {
            checkForConcreteDependency(returnType, "return type", ctx.start.line)
        }

        // Поиск параметров метода
        val params = ctx.children?.filter {
            it.text.matches(Regex("[A-Z][a-zA-Z0-9_]*")) && !it.text.startsWith("return")
        } ?: emptyList()

        for (param in params) {
            checkForConcreteDependency(param.text, "method parameter", ctx.start.line)
        }
    }


    override fun enterExpression(ctx: Java20Parser.ExpressionContext) {
        val text = ctx.text ?: return

        if (text.startsWith("new ")) {
            val typeName = ctx.children?.getOrNull(1)?.text ?: return
            checkForConcreteDependency(typeName, "new instance", ctx.start.line)
        }

        if (text.contains('(') && text.contains(')')) {
            val className = text.substringBefore('.')
            if (className.matches(Regex("[A-Z][A-Za-z0-9]*"))) {
                checkForConcreteDependency(className, "method invocation", ctx.start.line)
            }
        }
    }

    private fun checkForConcreteDependency(typeName: String, context: String, lineNumber: Int) {
        val cleanType = typeName.replace(Regex("<.*?>"), "").split(',').first().trim()

        if (isConcreteDependency(cleanType)) {
            currentConcreteDeps.add(
                ConcreteDependency(
                    dependencyType = cleanType,
                    usedIn = context,
                    lineNumber = lineNumber
                )
            )

            if (isDIPViolation(cleanType)) {
                currentDIPViolations.add(
                    DIPViolation(
                        highLevelModule = currentClass ?: "Unknown",
                        lowLevelModule = cleanType,
                        dependencyType = context,
                        lineNumber = lineNumber
                    )
                )
            }
        }
    }

    private fun isConcreteDependency(typeName: String): Boolean {
        if (typeName in primitiveTypes || typeName.startsWith("java.")) {
            return false
        }

        return importedTypes.any { it.endsWith(typeName) }
                || (currentPackage != null && !typeName.contains('.'))
    }

    private fun isDIPViolation(typeName: String): Boolean {
        val lowLevelPackage = importedTypeToPackageMap[typeName] ?: return false
        val highLevelPackage = currentPackage ?: return false

        val highLevelDepth = highLevelPackage.split('.').size
        val lowLevelDepth = lowLevelPackage.split('.').size

        // Для отладки
        println("DEBUG: highLevelPackage = $highLevelPackage, lowLevelPackage = $lowLevelPackage")
        println("DEBUG: highLevelDepth = $highLevelDepth, lowLevelDepth = $lowLevelDepth")

        return highLevelDepth >= lowLevelDepth
    }

    companion object {
        private val primitiveTypes = setOf(
            "byte", "short", "int", "long", "float", "double", "char", "boolean", "void",
            "String", "List", "Set", "Map"
        )

        fun analyze(tokens: CommonTokenStream): DependencyWalker {
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = DependencyWalker()
            walker.walk(listener, tree)

            return listener
        }
    }
}
