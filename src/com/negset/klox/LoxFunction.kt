package com.negset.klox

class LoxFunction(
    private val declaration: Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) : LoxCallable {
    override val arity = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure).apply {
            repeat(arity) {
                define(declaration.params[it].lexeme, arguments[it])
            }
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: ReturnThrowable) {
            if (isInitializer) return closure.getAt(0, "this")
            return returnValue.value
        }

        if (isInitializer) return closure.getAt(0, "this")
        return null
    }

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure).apply {
            define("this", instance)
        }
        return LoxFunction(declaration, environment, isInitializer)
    }

    override fun toString() = "<fn ${declaration.name.lexeme}>"
}