package com.negset.klox

import com.negset.klox.TokenType.*

class Parser(private val tokens: List<Token>) {
    private var current = 0

    private class ParseError : RuntimeException()

    fun parse(): Expr? {
        return try {
            expression()
        } catch (_: ParseError) {
            null
        }
    }

    private fun expression(): Expr {
        return equality()
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
        when {
            match(FALSE) -> return Literal(false)
            match(TRUE) -> return Literal(true)
            match(NIL) -> return Literal(null)

            match(NUMBER, STRING) -> return Literal(previous().literal)

            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                return Grouping(expr)
            }
        }

        throw parseErr(peek(), "Expect expression.")
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

        throw parseErr(peek(), message)
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

    private fun parseErr(token: Token, message: String): ParseError {
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