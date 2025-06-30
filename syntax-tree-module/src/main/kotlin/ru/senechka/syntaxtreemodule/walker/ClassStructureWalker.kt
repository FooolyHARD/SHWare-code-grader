import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.*
import grammar.Java20Lexer
import grammar.Java20Parser
import grammar.Java20ParserBaseListener

class ClassStructureWalker : Java20ParserBaseListener() {

    data class ClassInfo(
        val name: String,
        val isAbstract: Boolean,
        val isFinal: Boolean,
        val fields: List<FieldInfo>,
        val methods: List<MethodInfo>,
        val constructors: List<ConstructorInfo>,
        val lineNumber: Int,
        val typeParameters: List<String> = emptyList(),
        val superClass: String? = null,
        val interfaces: List<String> = emptyList()
    )

    data class FieldInfo(
        val name: String,
        val type: String,
        val modifiers: Set<String>,
        val lineNumber: Int
    )

    data class MethodInfo(
        val name: String,
        val returnType: String,
        val parameters: List<ParameterInfo>,
        val modifiers: Set<String>,
        val isAbstract: Boolean,
        val lineNumber: Int
    )

    data class ConstructorInfo(
        val parameters: List<ParameterInfo>,
        val modifiers: Set<String>,
        val lineNumber: Int
    )

    data class ParameterInfo(
        val name: String,
        val type: String
    )

    private val classes = mutableListOf<ClassInfo>()
    private var currentClass: ClassInfo? = null
    private var currentFields = mutableListOf<FieldInfo>()
    private var currentMethods = mutableListOf<MethodInfo>()
    private var currentConstructors = mutableListOf<ConstructorInfo>()

    fun getClassInfo(): List<ClassInfo> = classes.toList()

    override fun enterNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        val modifiers = ctx.classModifier().map { it.text }.toSet()

        currentClass = ClassInfo(
            name = extractClassName(ctx),
            isAbstract = modifiers.contains("abstract"),
            isFinal = modifiers.contains("final"),
            fields = emptyList(),
            methods = emptyList(),
            constructors = emptyList(),
            lineNumber = ctx.start.line,
            typeParameters = extractTypeParameters(ctx),
            superClass = extractSuperClass(ctx),
            interfaces = extractInterfaces(ctx)
        )

        currentFields.clear()
        currentMethods.clear()
        currentConstructors.clear()
    }

    private fun extractClassName(ctx: Java20Parser.NormalClassDeclarationContext): String {
        return ctx.children
            .filterIsInstance<Java20Parser.TypeIdentifierContext>()
            .firstOrNull()
            ?.let {
                it.children.firstOrNull { child -> child is TerminalNode }?.text
                    ?: it.text
            } ?: "Anonymous"
    }

    private fun extractTypeParameters(ctx: Java20Parser.NormalClassDeclarationContext): List<String> {
        return ctx.typeParameters()?.typeParameterList()?.typeParameter()
            ?.mapNotNull { param ->
                param.children
                    .filterIsInstance<Java20Parser.TypeIdentifierContext>()
                    .firstOrNull()
                    ?.text
            } ?: emptyList()
    }

    private fun extractSuperClass(ctx: Java20Parser.NormalClassDeclarationContext): String? {
        return ctx.classExtends()?.classType()?.let { classType ->
            classType.children
                .filter { it is TerminalNode || it is Java20Parser.TypeIdentifierContext }
                .joinToString("") { it.text }
        }
    }

    private fun extractInterfaces(ctx: Java20Parser.NormalClassDeclarationContext): List<String> {
        return ctx.classImplements()?.interfaceTypeList()?.interfaceType()
            ?.mapNotNull { it.classType()?.text }
            ?: emptyList()
    }

    override fun exitNormalClassDeclaration(ctx: Java20Parser.NormalClassDeclarationContext) {
        currentClass?.let {
            classes.add(it.copy(
                fields = currentFields.toList(),
                methods = currentMethods.toList(),
                constructors = currentConstructors.toList()
            ))
        }
        currentClass = null
    }

    override fun enterFieldDeclaration(ctx: Java20Parser.FieldDeclarationContext) {

        val modifiers = ctx.fieldModifier().map { it.text }.toSet()
        val type = ctx.unannType()?.text ?: UNKNOWN

        ctx.variableDeclaratorList().variableDeclarator().forEach { declarator ->
            val name = declarator.variableDeclaratorId().children
                .filterIsInstance<Java20Parser.IdentifierContext>()
                .firstOrNull()
                ?.text ?: UNKNOWN

            currentFields.add(FieldInfo(
                name = name,
                type = type,
                modifiers = modifiers,
                lineNumber = ctx.start.line
            ))
        }
    }

    override fun enterMethodDeclaration(ctx: Java20Parser.MethodDeclarationContext) {
        val modifiers = ctx.methodModifier().map { it.text }.toSet()
        val methodHeader = ctx.methodHeader()
        val methodDeclarator = methodHeader.methodDeclarator()

        currentMethods.add(MethodInfo(
            name = methodDeclarator.identifier().text,
            returnType = methodHeader.result().text,
            parameters = extractMethodParameters(methodDeclarator),
            modifiers = modifiers,
            isAbstract = modifiers.contains("abstract") || ctx.methodBody().isEmpty,
            lineNumber = ctx.start.line
        ))
    }

    private fun extractMethodParameters(declarator: Java20Parser.MethodDeclaratorContext): List<ParameterInfo> {
        return declarator.formalParameterList()?.let { paramList ->
            paramList.formalParameter().map { param ->
                ParameterInfo(
                    name = param.variableDeclaratorId().identifier().text,
                    type = param.unannType().text
                )
            }
        } ?: emptyList()
    }

    override fun enterConstructorDeclaration(ctx: Java20Parser.ConstructorDeclarationContext) {
        currentConstructors.add(ConstructorInfo(
            parameters = extractConstructorParameters(ctx.constructorDeclarator()),
            modifiers = ctx.constructorModifier().map { it.text }.toSet(),
            lineNumber = ctx.start.line
        ))
    }

    private fun extractConstructorParameters(declarator: Java20Parser.ConstructorDeclaratorContext): List<ParameterInfo> {
        return declarator.formalParameterList()?.let { paramList ->
            paramList.formalParameter().map { param ->
                ParameterInfo(
                    name = param.variableDeclaratorId().identifier().text,
                    type = param.unannType().text
                )
            }
        } ?: emptyList()
    }

    companion object {
        private const val UNKNOWN = "unknown"
        fun analyze(path: CharStream): List<ClassInfo> {
            val lexer = Java20Lexer(path)
            val tokens = CommonTokenStream(lexer)
            val parser = Java20Parser(tokens)
            val tree = parser.compilationUnit()

            val walker = ParseTreeWalker()
            val listener = ClassStructureWalker()
            walker.walk(listener, tree)

            return listener.getClassInfo()
        }
    }


}