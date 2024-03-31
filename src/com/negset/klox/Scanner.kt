package com.negset.klox

private val keywords = mapOf(
    "and" to TokenType.AND,
    "class" to TokenType.CLASS,
    "else" to TokenType.ELSE,
    "false" to TokenType.FALSE,
    "for" to TokenType.FOR,
    "fun" to TokenType.FUN,
    "if" to TokenType.IF,
    "nil" to TokenType.NIL,
    "or" to TokenType.OR,
    "print" to TokenType.PRINT,
    "return" to TokenType.RETURN,
    "super" to TokenType.SUPER,
    "this" to TokenType.THIS,
    "true" to TokenType.TRUE,
    "var" to TokenType.VAR,
    "while" to TokenType.WHILE,
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

        tokens.addLast(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> if (match('/')) {
                // A comment goes until the end of the line.
                while (peek() != '\n' && !isAtEnd()) advance()
            } else if (match('*')) {
                blockComment()
            } else {
                addToken(TokenType.SLASH)
            }

            // Ignore whitespace.
            ' ', '\r', '\t' -> {}
            '\n' -> line++

            '"' -> string()

            else -> when {
                c.isDigit() -> number()
                c.isAlpha() -> identifier()
                else -> err(line, "Unexpected character.")
            }
        }
    }

    private fun blockComment() {
        while (!(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
            // nested block comment.
            if (peek() == '/' && peekNext() == '*') {
                advance()
                advance()
                blockComment()
            } else if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            err(line, "Unterminated comment.")
            return
        }

        // The closing "*/".
        advance()
        advance()
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            err(line, "Unterminated string.")
            return
        }

        // The closing '"'.
        advance()

        // Trim the surrounding quotes.
        addToken(TokenType.STRING, source.substring(start + 1, current - 1))
    }

    private fun number() {
        while (peek().isDigit()) advance()

        // Look for a fractional part.
        if (peek() == '.' && peekNext().isDigit()) {
            // Consume the '.'
            advance()

            while (peek().isDigit()) advance()
        }

        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }

    private fun identifier() {
        while (peek().isAlphaOrDigit()) advance()

        val text = source.substring(start, current)
        val type = keywords.getOrDefault(text, TokenType.IDENTIFIER)
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