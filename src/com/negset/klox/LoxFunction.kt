package com.negset.klox

class LoxFunction(private val declaration: Function, private val closure: Environment) : LoxCallable {
    override val arity = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure).apply {
            (0..<arity).forEach {
                define(declaration.params[it].lexeme, arguments[it])
            }
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: ReturnThrowable) {
            return returnValue.value
        }
        return null
    }

    override fun toString() = "<fn ${declaration.name.lexeme}>"
}