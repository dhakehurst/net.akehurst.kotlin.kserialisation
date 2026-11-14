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

/** Thrown when a JSON value is used as the wrong kind of value, or a reference cannot be resolved. */
class JsonException : RuntimeException {
    constructor(message: String) : super(message)
}

/**
 * A JSON document: a [root] value plus an index of the objects that can be the target of a reference.
 *
 * @property identity a name for the document, used for identification only
 */
data class JsonDocument(
    val identity: String
) {
    /**
     * The values of the `$kind` entry, which says how a serialised object is to be interpreted.
     * Each is written as its name prefixed with `$`, e.g. `$OBJECT`.
     */
    enum class ComplexObjectKind {
        SINGLETON, SPECIAL, LOOKUP, PRIMITIVE, ENUM, ARRAY, OBJECT, LIST, SET, MAP;

        val asJsonString get() = JsonString("\$${this.name}")
    }

    companion object {
        /** Key of the entry giving the [ComplexObjectKind] of a serialised object. */
        val KIND = "\$kind"     // SINGLETON | PRIMITIVE | OBJECT | LIST | SET | MAP
        /** Key of the entry giving the qualified type name of a serialised object (or the lookup name for `$LOOKUP`). */
        val CLASS = "\$class"
        /** Key of the entry giving the key of a map entry. */
        val KEY = "\$key"
        /** Key of the entry giving the value of a primitive, enum, value type, lookup or map entry. */
        val VALUE = "\$value"
        /** Key of the entry giving the elements of a list, set or array. */
        val ELEMENTS = "\$elements"
        /** Key of the entry giving the entries of a map. */
        val ENTRIES = "\$entries"
    }

    /** The objects that can be the target of a reference, indexed by their path from the root. */
    val references = mutableMapOf<List<String>, JsonValue>()

    /** The root value of the document. */
    var root: JsonValue = JsonUnreferencableObject()

    fun toStringJson(): String {
        return this.root.toStringJson()
    }

    fun toFormattedJsonString(indent: String = "  ", increment: String = "  "): String {
        return this.root.toFormattedJsonString(indent, increment)
    }
}

/**
 * Base class of all JSON values.
 * The `as...` functions return this value as the requested kind, or throw [JsonException] if it is not of that kind.
 */
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

    /** Returns this value as compact JSON text. */
    abstract fun toStringJson(): String
    /** Returns this value as indented JSON text, starting at [indent] and indenting nested values by [increment]. */
    abstract fun toFormattedJsonString(indent: String, increment: String): String
}

/** A JSON object: an ordered map of property names to values. */
abstract class JsonObject : JsonValue() {
    /** The properties of this object, in the order they were set. */
    var property: Map<String, JsonValue> = mutableMapOf()

    override fun asObject(): JsonObject {
        return this
    }

    /** Sets the property [key] to [value] and returns this object. */
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

/** A JSON object that cannot be the target of a reference. */
class JsonUnreferencableObject : JsonObject() {

    override fun asObject(): JsonUnreferencableObject = this

    override fun setProperty(key: String, value: JsonValue): JsonUnreferencableObject = super.setProperty(key, value) as JsonUnreferencableObject

}

/**
 * A JSON object that can be the target of a reference.
 * It adds itself to the [document]'s references, under [path], when created.
 */
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

/**
 * A reference to an object written elsewhere in the same document, written as `{ "$ref" : "/a/b" }`.
 *
 * @property refPath the path, from the root, of the referenced object
 */
data class JsonReference(
    val document: JsonDocument,
    val refPath: List<String>
) : JsonValue() {

    /**
     * The referenced value.
     *
     * @throws JsonException if there is no referencable object at [refPath]
     */
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

/** A JSON boolean value. */
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

/** A JSON number, held as its textual form so that no precision is lost until it is converted. */
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

/** A JSON string value. [value] is the unescaped string. */
data class JsonString(
    val value: String
) : JsonValue() {

    companion object {
        private val unescape_b_regex = Regex("(?<!\\\\)\\\\b")
        private val unescape_f_regex = Regex("(?<!\\\\)\\\\f")
        private val unescape_n_regex = Regex("(?<!\\\\)\\\\n")
        private val unescape_r_regex = Regex("(?<!\\\\)\\\\r")
        private val unescape_t_regex = Regex("(?<!\\\\)\\\\t")
        /** Escapes [rawString] for writing in a JSON string literal. */
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

        /** Reverses [encode], returning the unescaped string. */
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

/** A JSON array. */
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

    /** Appends [element] and returns this array. */
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

/** The JSON `null` value. */
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