package net.akehurst.kotlin.kserialisation.json

import net.akehurst.kotlinx.collections.Stack

class JsonParserException : RuntimeException {
    constructor(message: String) : super(message)
}

object JsonParser {


        val TOKEN_WHITESPACE = Regex("\\s+", RegexOption.MULTILINE)
        val TOKEN_STRING = Regex("\"(?:\\\\?(.|\n))*?\"", RegexOption.MULTILINE)
        val TOKEN_NULL = "null"
        val TOKEN_NUMBER = Regex("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")
        val TOKEN_BOOLEAN = Regex("true|false", RegexOption.IGNORE_CASE)
        val TOKEN_ARRAY_START = "["
        val TOKEN_ARRAY_END = "]"
        val TOKEN_OBJECT_START = "{"
        val TOKEN_OBJECT_END = "}"
        val TOKEN_PROPERTY_SEP = ":"
        val TOKEN_SEP = ","


    class SimpleScanner(private val input: CharSequence) {
        var position: Int = 0

        fun hasMore(): Boolean {
            return this.position < this.input.length
        }

        fun hasNext(literal: String): Boolean {
            return this.input.startsWith(literal, this.position, false)
        }

        fun hasNext(pattern: Regex): Boolean {
            val lookingAt = pattern.find(this.input, this.position)?.range?.start == this.position
            return lookingAt
        }

        fun next(literal: String) : String{
            //assumes hasNext already called
            this.position += literal.length
            return literal
        }

        fun next(pattern: Regex): String {
            val m = pattern.find(this.input, this.position)
            val lookingAt = (m?.range?.start == this.position)
            if (lookingAt) {
                val match = m?.value ?: throw JsonParserException("Should never happen")
                this.position += m.value.length
                return match
            } else {
                throw JsonParserException("Error scanning for pattern ${pattern} at Position ${this.position}")
            }
        }
    }

    fun process(input: String) : JsonValue {
        val scanner = SimpleScanner(input)
        val nameStack = Stack<String>()
        val valueStack = Stack<JsonValue>()
        while (scanner.hasMore()) {
            when {
                scanner.hasNext(TOKEN_WHITESPACE) -> scanner.next(TOKEN_WHITESPACE)
                scanner.hasNext(TOKEN_NULL) -> {
                    scanner.next(TOKEN_NULL)
                    valueStack.push(JsonNull)
                }
                scanner.hasNext(TOKEN_BOOLEAN) -> {
                    val value = scanner.next(TOKEN_BOOLEAN)
                    valueStack.push(JsonBoolean(value.toBoolean()))
                }
                scanner.hasNext(TOKEN_NUMBER) -> {
                    val value = scanner.next(TOKEN_NUMBER)
                    valueStack.push(JsonNumber(value))
                }
                scanner.hasNext(TOKEN_STRING) -> {
                    val value = scanner.next(TOKEN_STRING)
                    valueStack.push(JsonString(value.drop(1).dropLast(1)))
                }
                scanner.hasNext(TOKEN_ARRAY_START) -> {
                    scanner.next(TOKEN_ARRAY_START)
                    valueStack.push(JsonArray())
                    //check for empty array
                    while(scanner.hasMore() && scanner.hasNext(TOKEN_WHITESPACE)) {
                        scanner.next(TOKEN_WHITESPACE)
                    }
                    if (scanner.hasNext(TOKEN_ARRAY_END)) {
                        scanner.next(TOKEN_ARRAY_END)
                    }
                }
                scanner.hasNext(TOKEN_ARRAY_END) -> {
                    scanner.next(TOKEN_ARRAY_END)
                    val value = valueStack.pop()
                    val peek = valueStack.peek()
                    if (peek is JsonArray) {
                        peek.addElement(value)
                    } else {
                        throw JsonParserException("Expected an Array but was a ${peek::class.simpleName}")
                    }
                }
                scanner.hasNext(TOKEN_SEP) -> {
                    scanner.next(TOKEN_SEP)
                    val value = valueStack.pop()
                    val peek = valueStack.peek()
                    when(peek) {
                        is JsonArray -> peek.addElement(value)
                        is JsonObject -> {
                            val name = nameStack.pop()
                            peek.setProperty(name, value)
                        }
                        else -> throw JsonParserException("Expected an Array or an Object but was a ${peek::class.simpleName}")
                    }
                }
                scanner.hasNext(TOKEN_OBJECT_START) -> {
                    scanner.next(TOKEN_OBJECT_START)
                    valueStack.push(JsonObject())
                    //check for empty object
                    while(scanner.hasMore() && scanner.hasNext(TOKEN_WHITESPACE)) {
                        scanner.next(TOKEN_WHITESPACE)
                    }
                    if (scanner.hasNext(TOKEN_OBJECT_END)) {
                        scanner.next(TOKEN_OBJECT_END)
                    }
                }
                scanner.hasNext(TOKEN_OBJECT_END) -> {
                    scanner.next(TOKEN_OBJECT_END)
                    val value = valueStack.pop()
                    val peek = valueStack.peek()
                    if (peek is JsonObject) {
                        val name = nameStack.pop()
                        peek.setProperty(name, value)
                        if (1==peek.property.size && peek.property.containsKey(Json.REF)) {
                            val ref = JsonReference(peek.property[Json.REF]!!.asString().value)
                            valueStack.pop()
                            valueStack.push(ref)
                        }
                    } else {
                        throw JsonParserException("Expected an Object but was a ${peek::class.simpleName}")
                    }
                }
                scanner.hasNext(TOKEN_PROPERTY_SEP) -> {
                    scanner.next(TOKEN_PROPERTY_SEP)
                    val name = valueStack.pop().asString().value
                    nameStack.push(name)
                }
                else -> throw JsonParserException("Unexpected character at position ${scanner.position} - '${input.substring(scanner.position)}'")
            }
        }
        return valueStack.pop()
    }
}