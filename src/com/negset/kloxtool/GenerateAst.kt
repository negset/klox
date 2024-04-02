package com.negset.kloxtool

import java.io.IOException
import java.io.PrintWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }
    val outputDir = args[0]
    defineAst(
        outputDir, "Expr", listOf(
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Any? value",
            "Unary    : Token operator, Expr right",
        )
    )
}

@Throws(IOException::class)
private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    PrintWriter(path, "UTF-8").run {
        println("package com.negset.klox")
        println()
        println("sealed interface $baseName {")
        println("    fun <R> accept(visitor: Visitor<R>): R")
        println()
        defineVisitor(this, baseName, types)
        println("}")

        // The AST classes.
        for (type in types) {
            println()
            val (className, fields) = type.split(":").map { it.trim() }
            defineType(this, baseName, className, fields)
        }

        close()
    }
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
    writer.run {
        println("    interface Visitor<R> {")

        for (type in types) {
            val typeName = type.split(":")[0].trim()
            println("        fun visit$typeName$baseName(${baseName.lowercase()}: $typeName): R")
        }

        println("    }")
    }
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fields: String) {
    val properties = fields.replace(Regex("(\\w+\\??)\\s(\\w+)"), "val $2: $1")

    writer.run {
        println("class $className($properties) : $baseName {")

        println("    override fun <R> accept(visitor: $baseName.Visitor<R>): R {")
        println("        return visitor.visit$className$baseName(this)")
        println("    }")

        println("}")
    }
}
