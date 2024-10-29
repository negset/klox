package com.negset.klox

class LoxClass(val name: String): LoxCallable {
    override val arity = 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        val instance = LoxInstance(this)
        return instance
    }

    override fun toString() = name
}