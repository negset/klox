package com.negset.klox

import com.negset.klox.TokenType.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    val globals = Environment()
    private var environment = globals
    private val locals = mutableMapOf<Expr, Int>()

    init {
        globals.define("clock", object : LoxCallable {
            override val arity = 0

            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
                return System.currentTimeMillis().toDouble() / 1000.0
            }

            override fun toString() = "<native fn>"
        })
    }

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach(::execute)
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
    }

    override fun visitAssignExpr(expr: Assign): Any? {
        val value = evaluate(expr.value)

        val distance = locals[expr]
        if (distance == null) {
            globals.assign(expr.name, value)
        } else {
            environment.assignAt(distance, expr.name, value)
        }
        return value
    }

    override fun visitBinaryExpr(expr: Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            BANG_EQUAL -> left != right
            EQUAL_EQUAL -> left == right
            GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) > (right as Double)
            }

            GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) >= (right as Double)
            }

            LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < (right as Double)
            }

            LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) <= (right as Double)
            }

            MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) - (right as Double)
            }

            PLUS -> when {
                left is Double && right is Double -> left + right
                left is String && right is String -> left + right
                else -> throw RuntimeError(
                    expr.operator,
                    "Operands of ${expr.operator.type} be two numbers or two strings"
                )
            }

            SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) / (right as Double)
            }

            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }

            else -> null
        }
    }

    override fun visitCallExpr(expr: Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = expr.arguments.map(::evaluate)

        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        }
        if (arguments.size != callee.arity) {
            throw RuntimeError(expr.paren, "Expected ${callee.arity} arguments but got ${arguments.size}.")
        }
        return callee.call(this, arguments)
    }

    override fun visitGetExpr(expr: Get): Any? {
        val obj = evaluate(expr.obj)
        if (obj is LoxInstance) {
            return obj[expr.name]
        }

        throw RuntimeError(expr.name, "Only instances have properties.")
    }

    override fun visitGroupingExpr(expr: Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal): Any? {
        return expr.value
    }

    override fun visitSetExpr(expr: Set): Any? {
        val obj = evaluate(expr.obj)

        if (obj !is LoxInstance) {
            throw RuntimeError(expr.name, "Only instances have fields.")
        }

        val value = evaluate(expr.value)
        obj[expr.name] = value
        return value
    }

    override fun visitLogicalExpr(expr: Logical): Any? {
        val left = evaluate(expr.left)

        when (expr.operator.type) {
            OR -> if (isTruthy(left)) return left
            AND -> if (!isTruthy(left)) return left
            else -> return null     // Unreachable.
        }

        return evaluate(expr.right)
    }

    override fun visitUnaryExpr(expr: Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            BANG -> !isTruthy(right)
            MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }

            else -> null    // Unreachable.
        }
    }

    override fun visitVariableExpr(expr: Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance == null) {
            globals.get(name)
        } else {
            environment.getAt(distance, name.lexeme)
        }
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun isTruthy(value: Any?) = when (value) {
        // false and nil are falsy, otherwise truthy
        null -> false
        is Boolean -> value
        else -> true
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand of ${operator.type} must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands of ${operator.type} must be numbers.")
    }

    private fun stringify(value: Any?) = when (value) {
        null -> "nil"
        is Double -> value.toString().removeSuffix(".0")
        is String -> value
        else -> value.toString()
    }

    override fun visitBlockStmt(stmt: Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitClassStmt(stmt: Class) {
        environment.define(stmt.name.lexeme, null)
        val cls = LoxClass(stmt.name.lexeme)
        environment.assign(stmt.name, cls)
    }

    override fun visitExpressionStmt(stmt: Expression) {
        evaluate(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Function) {
        val function = LoxFunction(stmt, environment)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitIfStmt(stmt: If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else {
            stmt.elseBranch?.let(::execute)
        }
    }

    override fun visitPrintStmt(stmt: Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Return) {
        val value = stmt.value?.let(::evaluate)
        throw ReturnThrowable(value)
    }

    override fun visitVarStmt(stmt: Var) {
        val value = stmt.initializer?.let(::evaluate)
        environment.define(stmt.name.lexeme, value)
    }

    override fun visitWhileStmt(stmt: While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            statements.forEach(::execute)
        } finally {
            this.environment = previous
        }
    }

    fun resolve(expr: Expr, distance: Int) {
        locals += expr to distance
    }
}