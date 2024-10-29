package com.negset.klox

class AstPrinter : Expr.Visitor<String>, Stmt.Visitor<String> {
    fun print(statements: List<Stmt>) {
        statements.forEach {
            println(it.accept(this))
        }
    }

    override fun visitAssignExpr(expr: Assign): String {
        return parenthesize("assign ${expr.name.lexeme}", expr.value)
    }

    override fun visitBinaryExpr(expr: Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitCallExpr(expr: Call): String {
        return parenthesize("call", expr.callee, *expr.arguments.toTypedArray())
    }

    override fun visitGetExpr(expr: Get): String {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal): String {
        return expr.value?.toString() ?: "nil"
    }

    override fun visitSetExpr(expr: Set): String {
        TODO("Not yet implemented")
    }

    override fun visitLogicalExpr(expr: Logical): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitUnaryExpr(expr: Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    override fun visitVariableExpr(expr: Variable): String {
        return expr.name.lexeme
    }

    override fun visitBlockStmt(stmt: Block): String {
        return StringBuilder().run {
            append("(block\n")
            stmt.statements.forEach {
                append(it.accept(this@AstPrinter))
                append("\n")
            }
            append(")")
            toString()
        }
    }

    override fun visitClassStmt(stmt: Class): String {
        TODO("Not yet implemented")
    }

    override fun visitExpressionStmt(stmt: Expression): String {
        return parenthesize("expr", stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Function): String {
        return StringBuilder().run {
            append("(fun ${stmt.name.lexeme} ")
            stmt.params.forEach { append(it.lexeme + " ") }
            append("\n")
            stmt.body.forEach { append(it.accept(this@AstPrinter) + "\n") }
            append(")")
            toString()
        }
    }

    override fun visitIfStmt(stmt: If): String {
        return StringBuilder().run {
            append("(if ")
            append(stmt.condition.accept(this@AstPrinter))
            append("\n")
            append(stmt.thenBranch.accept(this@AstPrinter))
            append("\n")
            if (stmt.elseBranch != null) {
                append("else\n")
                append(stmt.elseBranch.accept(this@AstPrinter))
            }
            append(")")
            toString()
        }
    }

    override fun visitPrintStmt(stmt: Print): String {
        return parenthesize("print", stmt.expression)
    }

    override fun visitReturnStmt(stmt: Return): String {
        return if (stmt.value != null)
            parenthesize(stmt.keyword.lexeme, stmt.value)
        else
            parenthesize(stmt.keyword.lexeme)
    }

    override fun visitVarStmt(stmt: Var): String {
        return if (stmt.initializer == null)
            parenthesize("var ${stmt.name.lexeme}")
        else
            parenthesize("var ${stmt.name.lexeme}", stmt.initializer)
    }

    override fun visitWhileStmt(stmt: While): String {
        return StringBuilder().run {
            append("(while ")
            append(stmt.condition.accept(this@AstPrinter))
            append("\n")
            append(stmt.body.accept(this@AstPrinter))
            append(")")
            toString()
        }
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        return StringBuilder().run {
            append("(").append(name)
            exprs.forEach {
                append(" ")
                append(it.accept(this@AstPrinter))
            }
            append(")")
            toString()
        }
    }
}