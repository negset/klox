package com.negset.klox

class Environment(val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values += name to value
    }

    fun assign(name: Token, value: Any?) {
        if (name.lexeme in values) {
            values[name.lexeme] = value
            return
        }

        enclosing?.run { return assign(name, value) }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun get(name: Token): Any? {
        if (name.lexeme in values) {
            return values[name.lexeme]
        }

        enclosing?.run { return get(name) }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}