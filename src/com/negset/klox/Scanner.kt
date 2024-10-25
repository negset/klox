package com.negset.klox

import com.negset.klox.TokenType.*

private val keywords = mapOf(
    "and" to AND,
    "class" to CLASS,
    "else" to ELSE,
    "false" to FALSE,
    "for" to FOR,
    "fun" to FUN,
    "if" to IF,
    "nil" to NIL,
    "or" to OR,
    "print" to PRINT,
    "return" to RETURN,
    "super" to SUPER,
    "this" to THIS,
    "true" to TRUE,
    "var" to VAR,
    "while" to WHILE,
)

class Scanner(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current
            scanToken()
        }

        tokens.addLast(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)
            '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
            '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)
            '/' -> if (match('/')) {
                // A comment goes until the end of the line.
                while (peek() != '\n' && !isAtEnd()) advance()
            } else {
                addToken(SLASH)
            }

            // Ignore whitespace.
            ' ', '\r', '\t' -> {}
            '\n' -> line++

            '"' -> string()

            else -> when {
                c.isDigit() -> number()
                c.isAlpha() -> identifier()
                else -> loxError(line, "Unexpected character.")
            }
        }
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            loxError(line, "Unterminated string.")
            return
        }

        // The closing '"'.
        advance()

        // Trim the surrounding quotes.
        addToken(STRING, source.substring(start + 1, current - 1))
    }

    private fun number() {
        while (peek().isDigit()) advance()

        // Look for a fractional part.
        if (peek() == '.' && peekNext().isDigit()) {
            // Consume the '.'
            advance()

            while (peek().isDigit()) advance()
        }

        addToken(NUMBER, source.substring(start, current).toDouble())
    }

    private fun identifier() {
        while (peek().isAlphaOrDigit()) advance()

        val text = source.substring(start, current)
        val type = keywords.getOrDefault(text, IDENTIFIER)
        addToken(type)
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens.addLast(Token(type, text, literal, line))
    }

    private fun isAtEnd() = current >= source.length

    private fun advance() = source[current++]

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        return true
    }

    private fun peek() = source.getOrNull(current) ?: Char.MIN_VALUE

    private fun peekNext() = source.getOrNull(current + 1) ?: Char.MIN_VALUE

    private fun Char.isAlpha() = this in 'a'..'z' || this in 'A'..'Z' || this == '_'

    private fun Char.isAlphaOrDigit() = isAlpha() || isDigit()
}