package com.negset.klox

import com.negset.klox.TokenType.*

class Interpreter : Expr.Visitor<Any?> {
    fun interpret(expr: Expr) {
        try {
            val value = evaluate(expr)
            println(stringify(value))
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
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

    override fun visitGroupingExpr(expr: Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal): Any? {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            BANG -> !isTruthy(right)
            MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }

            else -> null
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
        is String -> "\"$value\""
        else -> value.toString()
    }
}