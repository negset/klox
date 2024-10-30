package com.negset.klox

class LoxInstance(private val cls: LoxClass) {
    private val fields = mutableMapOf<String, Any?>()

    operator fun get(name: Token): Any? {
        if (name.lexeme in fields) {
            return fields[name.lexeme]
        }

        val method = cls.findMethod(name.lexeme)
        if (method != null) return method.bind(this)

        throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    operator fun set(name: Token, value: Any?) {
        fields += name.lexeme to value
    }

    override fun toString() = "${cls.name} instance"
}