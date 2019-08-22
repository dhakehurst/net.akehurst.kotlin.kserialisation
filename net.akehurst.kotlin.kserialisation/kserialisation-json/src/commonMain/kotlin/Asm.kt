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


package net.akehurst.kotlin.kserialisation.json

class JsonException : RuntimeException {
    constructor(message: String) : super(message)
}

data class JsonDocument(
        val identity: String
) {
    companion object {
        val TYPE = "\$type"     // PRIMITIVE | OBJECT | LIST | SET | MAP
        val CLASS = "\$class"
        val KEY = "\$key"
        val VALUE = "\$value"
        val ELEMENTS = "\$elements"

        val PRIMITIVE = JsonString("\$PRIMITIVE")
        val ARRAY = JsonString("\$ARRAY") // TODO: ?
        val OBJECT = JsonString("\$OBJECT")
        val LIST = JsonString("\$LIST")
        val MAP = JsonString("\$MAP")
        val SET = JsonString("\$SET")
    }

    val references = mutableMapOf<List<String>, JsonValue>()

    var root: JsonValue = JsonUnreferencableObject()

    fun toJsonString(): String {
        return this.root.toJsonString()
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

    abstract fun toJsonString(): String
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

    override fun toJsonString(): String {
        val elements = this.property.map {
            """"${it.key}":${it.value.toJsonString()}"""
        }.joinToString(",")
        return """{${elements}}"""
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
            return this.document.references[refPath] ?: throw JsonException("Reference target not found for path='$refPath'")
        }

    override fun asReference(): JsonReference {
        return this
    }

    override fun toJsonString(): String {
        val refPathStr = this.refPath.joinToString(separator="/", prefix = "/")
        return """{ "${Json.REF}" : "$refPathStr" }"""
    }
}

data class JsonBoolean(
        val value: Boolean
) : JsonValue() {
    override fun asBoolean(): JsonBoolean {
        return this
    }

    override fun toJsonString(): String {
        return """${this.value}"""
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

    override fun toJsonString(): String {
        return """${this._value}"""
    }
}

data class JsonString(
        val value: String
) : JsonValue() {
    override fun asString(): JsonString {
        return this
    }

    override fun toJsonString(): String {
        val escaped = this.value.replace("\"", "\\\"")
        return """"${this.value}""""
    }
}

class JsonArray : JsonValue() {

    var elements: List<JsonValue> = mutableListOf<JsonValue>()

    override fun asArray(): JsonArray {
        return this
    }

    override fun toJsonString(): String {
        val elements = this.elements.map {
            it.toJsonString()
        }.joinToString(",")
        return """[${elements}]"""
    }

    fun addElement(element: JsonValue): JsonArray {
        (this.elements as MutableList).add(element)
        return this
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other is JsonArray -> this.elements == other.elements
            else -> false
        }
    }
}

object JsonNull : JsonValue() {
    override fun toJsonString(): String {
        return "null"
    }

    override fun toString(): String {
        return "null"
    }
}