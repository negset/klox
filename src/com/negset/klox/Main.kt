package com.negset.klox

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.system.exitProcess

var hadError = false

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
    val expression = parser.parse()

    // Stop if there was a syntax error.
    if (hadError) return

    println(AstPrinter().print(expression!!))
}

fun report(line: Int, where: String, message: String) {
    System.err.println("[line $line] Error$where: $message")
    hadError = true
}

fun loxErr(line: Int, message: String) = report(line, "", message)

fun loxErr(token: Token, message: String) {
    if (token.type == TokenType.EOF) {
        report(token.line, " at end", message)
    } else {
        report(token.line, " at '${token.lexeme}'", message)
    }
}