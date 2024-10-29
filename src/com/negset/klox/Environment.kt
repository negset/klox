package com.negset.klox

class Environment(val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values += name to value
    }

    fun assign(name: Token, value: Any?) {
        if (name.lexeme in values) {
            values += name.lexeme to value
            return
        }

        enclosing?.run { return assign(name, value) }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values += name.lexeme to value
    }

    fun get(name: Token): Any? {
        if (name.lexeme in values) {
            return values[name.lexeme]
        }

        enclosing?.run { return get(name) }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    fun ancestor(distance: Int): Environment {
        var environment = this
        repeat(distance) {
            environment = environment.enclosing
                ?: error("Environment has no ancestor with distance $distance.")
        }

        return environment
    }
}