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

        fun next(literal: String): String {
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

    fun process(input: String): JsonDocument {
        val doc = JsonDocument("json")
        if (input.isEmpty()) {
            throw JsonParserException("Expected Json content but input was empty")
        }
        val scanner = SimpleScanner(input)
        val path = Stack<String>()
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
                    path.push("0") // path segment for first element
                    valueStack.push(JsonArray())
                    //check for empty array
                    while (scanner.hasMore() && scanner.hasNext(TOKEN_WHITESPACE)) {
                        scanner.next(TOKEN_WHITESPACE)
                    }
                    if (scanner.hasNext(TOKEN_ARRAY_END)) {
                        scanner.next(TOKEN_ARRAY_END)
                        path.pop()
                    }
                }
                scanner.hasNext(TOKEN_ARRAY_END) -> {
                    scanner.next(TOKEN_ARRAY_END)
                    path.pop()
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
                    path.pop()
                    val value = valueStack.pop()
                    val peek = valueStack.peek()
                    when (peek) {
                        is JsonArray -> {
                            peek.addElement(value)
                            path.push(peek.elements.size.toString())
                        }
                        is JsonObject -> {
                            val name = nameStack.pop()
                            peek.setProperty(name, value)
                        }
                        else -> throw JsonParserException("Expected an Array or an Object but was a ${peek::class.simpleName}")
                    }
                }
                scanner.hasNext(TOKEN_OBJECT_START) -> {
                    scanner.next(TOKEN_OBJECT_START)
                    valueStack.push(JsonUnreferencableObject())
                    //check for empty object
                    while (scanner.hasMore() && scanner.hasNext(TOKEN_WHITESPACE)) {
                        scanner.next(TOKEN_WHITESPACE)
                    }
                    if (scanner.hasNext(TOKEN_OBJECT_END)) {
                        scanner.next(TOKEN_OBJECT_END)
                    }
                }
                scanner.hasNext(TOKEN_OBJECT_END) -> {
                    scanner.next(TOKEN_OBJECT_END)
                    path.pop()
                    val value = valueStack.pop()
                    val peek = valueStack.peek()
                    if (peek is JsonObject) {
                        val name = nameStack.pop()
                        peek.setProperty(name, value)
                        // handle different kinds of object!
                        when {
                            // JsonReference
                            (1 == peek.property.size && peek.property.containsKey(Json.REF)) -> {
                                val refStr = peek.property[Json.REF]!!.asString().value
                                // remove leading '/' then split
                                val refStr1 = refStr.substring(1)
                                val refPath = if (refStr1.isEmpty()) emptyList<String>() else refStr1.split("/")
                                val ref = JsonReference(doc, refPath)
                                valueStack.pop()
                                valueStack.push(ref)
                            }
                            // JsonReferenceableObject
                            (peek.property.containsKey(JsonDocument.TYPE) && peek.property[JsonDocument.TYPE] == JsonDocument.OBJECT) -> {
                                val jPath = path.elements.filter {
                                    it != JsonDocument.ELEMENTS && it != JsonDocument.VALUE
                                }
                                val obj = JsonReferencableObject(doc, jPath)
                                valueStack.pop()
                                obj.property = peek.property
                                valueStack.push(obj)
                            }
                        }

                    } else {
                        throw JsonParserException("Expected an Object but was a ${peek::class.simpleName}")
                    }
                }
                scanner.hasNext(TOKEN_PROPERTY_SEP) -> {
                    scanner.next(TOKEN_PROPERTY_SEP)
                    val name = valueStack.pop().asString().value
                    nameStack.push(name)
                    path.push(name)
                }
                else -> throw JsonParserException("Unexpected character at position ${scanner.position} - '${input.substring(scanner.position)}'")
            }
        }
        doc.root = valueStack.pop()
        return doc
    }
}