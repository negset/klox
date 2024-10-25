package com.negset.klox

import com.negset.klox.TokenType.*

class Parser(private val tokens: List<Token>) {
    private var current = 0

    private class ParseError : RuntimeException()

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let { statements += it }
        }

        return statements
    }

    private fun declaration(): Stmt? {
        try {
            return if (match(VAR)) varDeclaration()
            else statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")
        val initializer = if (match(EQUAL)) expression() else null
        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Var(name, initializer)
    }

    private fun statement(): Stmt {
        return when {
            match(PRINT) -> printStatement()
            match(LEFT_BRACE) -> Block(block())
            else -> expressionStatement()
        }
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Print(value)
    }

    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()

        while (!checkType(RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements += it }
        }

        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Expression(expr)
    }

    private fun expression(): Expr {
        return assignment()
    }

    @Suppress("ThrowableNotThrown")
    private fun assignment(): Expr {
        val expr = equality()

        if (match(EQUAL)) {
            val equal = previous()
            val value = assignment()

            if (expr is Variable) {
                return Assign(expr.name, value)
            }

            parseError(equal, "Invalid assignment target.")
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(MINUS, PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(SLASH, STAR)) {
            val operator = previous()
            val right = unary()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Unary(operator, right)
        }

        return primary()
    }

    private fun primary(): Expr {
        return when {
            match(FALSE) -> Literal(false)
            match(TRUE) -> Literal(true)
            match(NIL) -> Literal(null)

            match(NUMBER, STRING) -> Literal(previous().literal)

            match(IDENTIFIER) -> Variable(previous())

            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                Grouping(expr)
            }

            else -> throw parseError(peek(), "Expect expression.")
        }
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (checkType(type)) {
                advance()
                return true
            }
        }

        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (checkType(type)) return advance()

        throw parseError(peek(), message)
    }

    private fun checkType(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd() = peek().type == EOF

    private fun peek() = tokens[current]

    private fun previous() = tokens[current - 1]

    private fun parseError(token: Token, message: String): ParseError {
        loxError(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return

            when (peek().type) {
                CLASS,
                FUN,
                VAR,
                FOR,
                IF,
                WHILE,
                PRINT,
                RETURN -> return

                else -> advance()
            }
        }
    }
}