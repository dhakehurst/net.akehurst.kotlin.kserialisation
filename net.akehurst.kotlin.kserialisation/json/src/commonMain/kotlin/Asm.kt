/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.kotlin.json

class JsonException : RuntimeException {
    constructor(message: String) : super(message)
}

data class JsonDocument(
    val identity: String
) {
    enum class ComplexObjectKind {
        SINGLETON, PRIMITIVE, ENUM, ARRAY, OBJECT, LIST, SET, MAP;

        val asJsonString get() = JsonString("\$${this.name}")
    }

    companion object {
        val KIND = "\$kind"     // SINGLETON | PRIMITIVE | OBJECT | LIST | SET | MAP
        val CLASS = "\$class"
        val KEY = "\$key"
        val VALUE = "\$value"
        val ELEMENTS = "\$elements"
        val ENTRIES = "\$entries"
    }

    val references = mutableMapOf<List<String>, JsonValue>()

    var root: JsonValue = JsonUnreferencableObject()

    fun toStringJson(): String {
        return this.root.toStringJson()
    }

    fun toFormattedJsonString(indent: String = "  ", increment: String = "  "): String {
        return this.root.toFormattedJsonString(indent, increment)
    }
}

abstract class JsonValue {

    open fun asBoolean(): JsonBoolean {
        throw JsonException("Object is not a JsonNumber")
    }

    open fun asNumber(): JsonNumber {
        throw JsonException("Object is not a JsonNumber")
    }

    open fun asString(): JsonString {
        throw JsonException("Object is not a JsonString")
    }

    open fun asObject(): JsonObject {
        throw JsonException("Object is not a JsonObject")
    }

    open fun asReference(): JsonReference {
        throw JsonException("Object is not a JsonReference")
    }

    open fun asArray(): JsonArray {
        throw JsonException("Object is not a JsonArray")
    }

    abstract fun toStringJson(): String
    abstract fun toFormattedJsonString(indent: String, increment: String): String
}

abstract class JsonObject : JsonValue() {
    var property: Map<String, JsonValue> = mutableMapOf()

    override fun asObject(): JsonObject {
        return this
    }

    open fun setProperty(key: String, value: JsonValue): JsonObject {
        (this.property as MutableMap)[key] = value
        return this
    }

    override fun toStringJson(): String {
        val elements = this.property.map {
            """"${it.key}":${it.value.toStringJson()}"""
        }.joinToString(",")
        return """{${elements}}"""
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        val elements = this.property.map {
            """$indent"${it.key}" : ${it.value.toFormattedJsonString(indent + increment, increment)}"""
        }.joinToString(",\n")
        return "{\n${elements}\n${indent.substringBeforeLast(increment)}}".trimMargin()
    }
}

class JsonUnreferencableObject : JsonObject() {

    override fun asObject(): JsonUnreferencableObject = this

    override fun setProperty(key: String, value: JsonValue): JsonUnreferencableObject = super.setProperty(key, value) as JsonUnreferencableObject

}

data class JsonReferencableObject(
    val document: JsonDocument,
    val path: List<String>
) : JsonObject() {

    init {
        this.document.references[path] = this
    }

    override fun asObject(): JsonReferencableObject = this

    override fun setProperty(key: String, value: JsonValue): JsonReferencableObject = super.setProperty(key, value) as JsonReferencableObject

}

data class JsonReference(
    val document: JsonDocument,
    val refPath: List<String>
) : JsonValue() {

    val target: JsonValue
        get() {
            return this.document.references[refPath] ?: throw JsonException("Reference target not found for path='${refPath.joinToString(",", prefix = "/")}'")
        }

    override fun asReference(): JsonReference {
        return this
    }

    override fun toStringJson(): String {
        val refPathStr = this.refPath.joinToString(separator = "/", prefix = "/")
        return """{ "${Json.REF}" : "$refPathStr" }"""
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toStringJson()
    }
}

data class JsonBoolean(
    val value: Boolean
) : JsonValue() {
    override fun asBoolean(): JsonBoolean {
        return this
    }

    override fun toStringJson(): String {
        return """${this.value}"""
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toStringJson()
    }
}

data class JsonNumber(
    private val _value: String
) : JsonValue() {
    fun toByte(): Byte {
        return this._value.toByte()
    }

    fun toShort(): Short {
        return this._value.toShort()
    }

    fun toInt(): Int {
        return this._value.toInt()
    }

    fun toLong(): Long {
        return this._value.toLong()
    }

    fun toFloat(): Float {
        return this._value.toFloat()
    }

    fun toDouble(): Double {
        return this._value.toDouble()
    }

    override fun asNumber(): JsonNumber {
        return this
    }

    override fun toStringJson(): String {
        return this._value
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toStringJson()
    }
}

data class JsonString(
    val value: String
) : JsonValue() {

    companion object {
        private val unescape_b_regex = Regex("(?<!\\\\)\\\\b")
        private val unescape_f_regex = Regex("(?<!\\\\)\\\\f")
        private val unescape_n_regex = Regex("(?<!\\\\)\\\\n")
        private val unescape_r_regex = Regex("(?<!\\\\)\\\\r")
        private val unescape_t_regex = Regex("(?<!\\\\)\\\\t")
        fun encode(rawString: String): String {
            return rawString
                .replace("\\", "\\\\")
                .replace("\b", "\\b")
                .replace("\u000C", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"")
        }

        fun decode(encodedValue: String): String {
            val value = encodedValue//.replace(Regex("(?<!\\\\)\\\\(.)"),"$1")
                .replace(unescape_b_regex, "\b")
                .replace(unescape_f_regex, "\u000C")
                .replace(unescape_n_regex, "\n")
                .replace(unescape_r_regex, "\r")
                .replace(unescape_t_regex, "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
            return value
        }
    }

    val encodedValue: String get() = encode(value)

    override fun asString(): JsonString = this

    override fun toStringJson(): String {
        return """"${this.encodedValue}""""
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toStringJson()
    }
}

class JsonArray : JsonValue() {

    var elements: List<JsonValue> = mutableListOf<JsonValue>()

    override fun asArray(): JsonArray {
        return this
    }

    override fun toStringJson(): String {
        val elements = this.elements.map {
            it.toStringJson()
        }.joinToString(",")
        return """[${elements}]"""
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return when (elements.size) {
            0 -> "[]"
            1 -> {
                val element = this.elements[0].toFormattedJsonString(indent + increment, increment)
                return "[ ${element} ]"
            }

            else -> {
                val elements = this.elements.map {
                    indent + it.toFormattedJsonString(indent + increment, increment)
                }.joinToString(",\n")
                return "[\n${elements}\n${indent.substringBeforeLast(increment)}]"
            }
        }
    }

    fun addElement(element: JsonValue): JsonArray {
        (this.elements as MutableList).add(element)
        return this
    }

    override fun hashCode(): Int = this.elements.hashCode()
    override fun equals(other: Any?): Boolean {
        return when {
            other is JsonArray -> this.elements == other.elements
            else -> false
        }
    }
}

object JsonNull : JsonValue() {
    override fun toStringJson(): String {
        return "null"
    }

    override fun toString(): String {
        return "null"
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toStringJson()
    }
}