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
            return when {
                match(CLASS) -> classDeclaration()
                match(FUN) -> function("function")
                match(VAR) -> varDeclaration()
                else -> statement()
            }
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun classDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect class name.")
        consume(LEFT_BRACE, "Expect '{' before class body.")

        val methods = mutableListOf<Function>()
        while (!checkType(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.")

        return Class(name, methods)
    }

    @Suppress("ThrowableNotThrown")
    private fun function(kind: String): Function {
        val name = consume(IDENTIFIER, "Expect $kind name.")
        consume(LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!checkType(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    parseError(peek(), "Can't have more than 255 parameters.")
                }
                parameters += consume(IDENTIFIER, "Expect parameter name.")
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.")

        consume(LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Function(name, parameters, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")
        val initializer = if (match(EQUAL)) expression() else null
        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Var(name, initializer)
    }

    private fun statement(): Stmt {
        return when {
            match(FOR) -> forStatement()
            match(IF) -> ifStatement()
            match(PRINT) -> printStatement()
            match(RETURN) -> returnStatement()
            match(WHILE) -> whileStatement()
            match(LEFT_BRACE) -> Block(block())
            else -> expressionStatement()
        }
    }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")
        val initializer = when {
            match(SEMICOLON) -> null
            match(VAR) -> varDeclaration()
            else -> expressionStatement()
        }

        val condition =
            if (!checkType(SEMICOLON)) expression()
            else Literal(true)
        consume(SEMICOLON, "Expect ';' after for condition.")

        val increment =
            if (!checkType(RIGHT_PAREN)) expression()
            else null
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = statement()

        increment?.let {
            body = Block(listOf(body, Expression(it)))
        }

        body = While(condition, body)

        initializer?.let {
            body = Block(listOf(it, body))
        }

        return body
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else null

        return If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (checkType(SEMICOLON)) null else expression()

        consume(SEMICOLON, "Expect ';' after return value.")
        return Return(keyword, value)
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after while condition.")
        val body = statement()

        return While(condition, body)
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
        val expr = or()

        if (match(EQUAL)) {
            val equal = previous()
            val value = assignment()

            when (expr) {
                is Variable -> {
                    return Assign(expr.name, value)
                }

                is Get -> {
                    return Set(expr.obj, expr.name, value)
                }

                else -> parseError(equal, "Invalid assignment target.")
            }
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(OR)) {
            val operator = previous()
            val right = and()
            expr = Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(AND)) {
            val operator = previous()
            val right = equality()
            expr = Logical(expr, operator, right)
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

        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            when {
                match(LEFT_PAREN) -> {
                    expr = finishCall(expr)
                }

                match(DOT) -> {
                    val name = consume(IDENTIFIER, "Expect property name after '.'.")
                    expr = Get(expr, name)
                }

                else -> {
                    break
                }
            }
        }

        return expr
    }

    @Suppress("ThrowableNotThrown")
    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!checkType(RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    parseError(peek(), "Can't have more than 255 arguments.")
                }
                arguments += expression()
            } while (match(COMMA))
        }

        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")

        return Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        return when {
            match(FALSE) -> Literal(false)
            match(TRUE) -> Literal(true)
            match(NIL) -> Literal(null)

            match(NUMBER, STRING) -> Literal(previous().literal)

            match(THIS) -> This(previous())

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