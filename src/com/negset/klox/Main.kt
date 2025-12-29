package com.negset.klox

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

val interpreter = Interpreter()
var hadError = false
var hadRuntimeError = false

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: klox [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        runFile(args[0])
    } else {
        runPrompt()
    }
}

@Throws(IOException::class)
fun runFile(path: String) {
    val bytes = Files.readAllBytes(Paths.get(path))
    runSource(String(bytes, Charset.defaultCharset()))

    // Indicate an error in the exit code.
    if (hadError) exitProcess(65)
    if (hadRuntimeError) exitProcess(70)
}

@Throws(IOException::class)
fun runPrompt() {
    val input = InputStreamReader(System.`in`)
    val reader = BufferedReader(input)

    while (true) {
        print("> ")
        val line = reader.readLine() ?: break
        runSource(line)
        hadError = false
    }
}

fun runSource(source: String) {
    val scanner = Scanner(source)
    val tokens = scanner.scanTokens()
    val parser = Parser(tokens)
    val statements = parser.parse()

    // Stop if there was a syntax error.
    if (hadError) return

    val resolver = Resolver(interpreter)
    resolver.resolve(statements)

    // Stop if there was a resolution error.
    if (hadError) return

    interpreter.interpret(statements)
//    AstPrinter().print(statements)
}

fun report(line: Int, where: String, message: String) {
    System.err.println("[line $line] Error$where: $message")
    hadError = true
}

fun loxError(line: Int, message: String) = report(line, "", message)

fun loxError(token: Token, message: String) {
    if (token.type == TokenType.EOF) {
        report(token.line, " at end", message)
    } else {
        report(token.line, " at '${token.lexeme}'", message)
    }
}

class RuntimeError(val token: Token, message: String) : RuntimeException(message)

fun runtimeError(error: RuntimeError) {
    println("${error.message}\n[line ${error.token.line}]")
    hadRuntimeError = true
}