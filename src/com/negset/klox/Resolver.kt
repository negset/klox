package com.negset.klox

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes = ArrayDeque<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE

    private enum class FunctionType { NONE, FUNCTION, INITIALIZER, METHOD }
    private enum class ClassType { NONE, CLASS }

    fun resolve(statements: List<Stmt>) {
        statements.forEach(::resolve)
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun scope(inScope: MutableMap<String, Boolean>.() -> Unit) {
        scopes += mutableMapOf()
        scopes.last().inScope()
        scopes.removeLast()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return
        val scope = scopes.last()
        if (name.lexeme in scope) {
            loxError(name, "Already a variable with this name in this scope.")
        }

        scope += name.lexeme to false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.last() += name.lexeme to true
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        scopes.reversed().forEachIndexed { distance, scope ->
            if (name.lexeme in scope) {
                interpreter.resolve(expr, distance)
                return
            }
        }
    }

    private fun resolveFunction(function: Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        scope {
            function.params.forEach {
                declare(it)
                define(it)
            }
            resolve(function.body)
        }

        currentFunction = enclosingFunction
    }

    override fun visitAssignExpr(expr: Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Call) {
        resolve(expr.callee)
        expr.arguments.forEach(::resolve)
    }

    override fun visitGetExpr(expr: Get) {
        resolve(expr.obj)
    }

    override fun visitGroupingExpr(expr: Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal) {
        // Nothing to do.
    }

    override fun visitSetExpr(expr: Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitLogicalExpr(expr: Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitThisExpr(expr: This) {
        if (currentClass == ClassType.NONE) {
            loxError(expr.keyword, "Can't use 'this' outside of a class.")
            return
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitUnaryExpr(expr: Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Variable) {
        if (!scopes.isEmpty() && scopes.last()[expr.name.lexeme] == false) {
            loxError(expr.name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitBlockStmt(stmt: Block) {
        scope {
            resolve(stmt.statements)
        }
    }

    override fun visitClassStmt(stmt: Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        scope {
            put("this", true)
            stmt.methods.forEach {
                val declaration =
                    if (it.name.lexeme == "init")
                        FunctionType.INITIALIZER
                    else
                        FunctionType.METHOD

                resolveFunction(it, declaration)
            }
        }

        currentClass = enclosingClass
    }

    override fun visitExpressionStmt(stmt: Expression) {
        resolve(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIfStmt(stmt: If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let(::resolve)
    }

    override fun visitPrintStmt(stmt: Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Return) {
        if (currentFunction == FunctionType.NONE) {
            loxError(stmt.keyword, "Can't return from top-level code.")
        }

        stmt.value?.let {
            if (currentFunction == FunctionType.INITIALIZER) {
                loxError(stmt.keyword, "Can't return a value from an initializer.")
            }

            resolve(it)
        }
    }

    override fun visitVarStmt(stmt: Var) {
        declare(stmt.name)
        stmt.initializer?.let(::resolve)
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }
}