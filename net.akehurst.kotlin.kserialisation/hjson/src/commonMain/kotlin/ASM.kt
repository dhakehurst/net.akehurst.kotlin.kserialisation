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

package net.akehurst.hjson

/** Thrown when a HJSON value is used as the wrong kind of value, or a reference cannot be resolved. */
class HJsonException : RuntimeException {
    constructor(message: String) : super(message)
}

/**
 * A HJSON document: a [root] value plus an index of the objects that can be the target of a reference.
 *
 * @property identity a name for the document, used for identification only
 */
class HJsonDocument(
    val identity: String
) {

    /**
     * The values of the `$kind` entry, which says how a serialised object is to be interpreted.
     * Each is written as its name prefixed with `$`, e.g. `$OBJECT`.
     */
    enum class ComplexObjectKind {
        SINGLETON, SPECIAL, LOOKUP, PRIMITIVE, ENUM, ARRAY, OBJECT, LIST, SET, MAP;

        val asHJsonString get() = HJsonString("\$${this.name}")
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

    /** Every value in the document, indexed by its path from the root. */
    val index = mutableMapOf<List<String>, HJsonValue>()
    /** The objects that can be the target of a reference, indexed by their path from the root. */
    val references = mutableMapOf<List<String>, HJsonValue>()

    /** The root value of the document. */
    var root: HJsonValue = HJsonUnreferencableObject()

    fun toJsonString(): String {
        return this.root.toJsonString()
    }

    fun toFormattedJsonString(indent: String = "  ", increment: String = "  "): String {
        return this.root.toFormattedJsonString(indent, increment)
    }

    fun toHJsonString(indent: String = "  ", increment: String = "  "): String {
        return this.root.toHJsonString(indent, increment)
    }

    override fun hashCode(): Int {
        return this.identity.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other is HJsonDocument -> this.root == other.root
            else -> false
        }
    }

    override fun toString(): String {
        return this.toHJsonString()
    }
}

/**
 * Base class of all HJSON values.
 * The `as...` functions return this value as the requested kind, or throw [HJsonException] if it is not of that kind.
 */
abstract class HJsonValue {

    open fun asBoolean(): HJsonBoolean {
        throw HJsonException("Object is not a HJsonNumber")
    }

    open fun asNumber(): HJsonNumber {
        throw HJsonException("Object is not a HJsonNumber")
    }

    open fun asString(): HJsonString {
        throw HJsonException("Object is not a HJsonString")
    }

    open fun asObject(): HJsonObject {
        throw HJsonException("Object is not a HJsonObject")
    }

    open fun asReference(): HJsonReference {
        throw HJsonException("Object is not a HJsonReference")
    }

    open fun asArray(): HJsonArray {
        throw HJsonException("Object is not a HJsonArray")
    }

    /** Returns this value as compact JSON text. */
    abstract fun toJsonString(): String
    /** Returns this value as indented JSON text, starting at [indent] and indenting nested values by [increment]. */
    abstract fun toFormattedJsonString(indent: String, increment: String): String
    /** Returns this value as indented HJSON text, starting at [indent] and indenting nested values by [increment]. */
    abstract fun toHJsonString(indent: String, increment: String): String

    abstract override fun hashCode(): Int
    abstract override fun equals(other: Any?): Boolean

    override fun toString(): String {
        return this.toHJsonString("  ", "  ")
    }
}

/** A HJSON object: an ordered map of property names to values. */
abstract class HJsonObject : HJsonValue() {
    /** The properties of this object, in the order they were set. */
    var property: Map<String, HJsonValue> = mutableMapOf()

    override fun asObject(): HJsonObject {
        return this
    }

    /** Sets the property [key] to [value] and returns this object. */
    open fun setProperty(key: String, value: HJsonValue): HJsonObject {
        (this.property as MutableMap)[key] = value
        return this
    }

    override fun toJsonString(): String {
        val elements = this.property.map {
            """"${it.key}":${it.value.toJsonString()}"""
        }.joinToString(",")
        return """{${elements}}"""
    }

    private fun keyToString(key: String): String {
        return when {
            HJson.KEY_WORDS.contains(key) -> "\"$key\""
            else -> key
        }
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        val elements = this.property.map {
            """$indent"${it.key}" : ${it.value.toFormattedJsonString(indent + increment, increment)}"""
        }.joinToString(",\n")
        return "{\n${elements}\n${indent.substringBeforeLast(increment)}}".trimMargin()
    }

    override fun toHJsonString(indent: String, increment: String): String {
        return when {
            0 == this.property.size -> "{}"
            else -> {
                val elements = this.property.map {
                    """$indent${keyToString(it.key)} : ${it.value.toHJsonString(indent + increment, increment)}"""
                }.joinToString("\n")
                "{\n${elements}\n${indent.substringBeforeLast(increment)}}".trimMargin()
            }
        }
    }

    override fun hashCode(): Int {
        return this.property.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other is HJsonObject -> other.property == this.property
            else -> false
        }
    }
}

/** A HJSON object that cannot be the target of a reference. */
class HJsonUnreferencableObject : HJsonObject() {

    override fun asObject(): HJsonUnreferencableObject = this

    override fun setProperty(key: String, value: HJsonValue): HJsonUnreferencableObject =
        super.setProperty(key, value) as HJsonUnreferencableObject
}

/**
 * A HJSON object that can be the target of a reference.
 * It adds itself to the [document]'s references, under [path], when created.
 */
data class HJsonReferencableObject(
    val document: HJsonDocument,
    val path: List<String>
) : HJsonObject() {

    init {
        this.document.references[path] = this
    }

    override fun asObject(): HJsonReferencableObject = this

    override fun setProperty(key: String, value: HJsonValue): HJsonReferencableObject =
        super.setProperty(key, value) as HJsonReferencableObject

    override fun hashCode(): Int = super.hashCode()
    override fun equals(other: Any?): Boolean = super.equals(other)
}

/**
 * A reference to an object written elsewhere in the same document, written as `{ $ref : "#/a/b" }`.
 *
 * @property refPath the path, from the root, of the referenced object
 */
data class HJsonReference(
    val document: HJsonDocument,
    val refPath: List<String>
) : HJsonValue() {

    companion object {
        fun stringToList(refPathStr: String): List<String> {
            val ref = refPathStr.substringAfter("#/")
            return when {
                ref.isBlank() -> emptyList()
                else -> ref.split("/")
            }
        }
    }

    constructor(document: HJsonDocument, refPathStr: String) : this(document, stringToList(refPathStr))

    /**
     * The referenced value.
     *
     * @throws HJsonException if there is no referencable object at [refPath]
     */
    val target: HJsonValue
        get() {
            return this.document.references[refPath] ?: throw HJsonException(
                "Reference target not found for path='${
                    refPath.joinToString(
                        ",",
                        prefix = "/"
                    )
                }'"
            )
        }

    override fun asReference(): HJsonReference {
        return this
    }

    override fun toJsonString(): String {
        val refPathStr = this.refPath.joinToString(separator = "/", prefix = "#/")
        return """{ "${HJson.REF}" : "$refPathStr" }"""
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toJsonString()
    }

    override fun toHJsonString(indent: String, increment: String): String {
        val refPathStr = this.refPath.joinToString(separator = "/", prefix = "#/")
        return """{ ${HJson.REF} : "$refPathStr" }"""
    }

    override fun hashCode(): Int = this.refPath.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is HJsonReference -> false
        else -> this.refPath == other.refPath
    }
}

/** A HJSON boolean value. */
data class HJsonBoolean(
    val value: Boolean
) : HJsonValue() {
    override fun asBoolean(): HJsonBoolean {
        return this
    }

    override fun toJsonString(): String {
        return "${this.value}"
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toJsonString()
    }

    override fun toHJsonString(indent: String, increment: String): String {
        return "${this.value}"
    }
}

/** A HJSON number, held as its textual form so that no precision is lost until it is converted. */
data class HJsonNumber(
    private val _value: String
) : HJsonValue() {
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

    override fun asNumber(): HJsonNumber {
        return this
    }

    override fun toJsonString(): String {
        return "${this._value}"
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toJsonString()
    }

    override fun toHJsonString(indent: String, increment: String): String {
        return this.toJsonString()
    }
}

/** A HJSON string value. [value] is the unescaped string. */
data class HJsonString(
    val value: String
) : HJsonValue() {

    companion object {
        fun decode(encodedValue: String): HJsonString {
            val value = encodedValue
                .replace("\\b", "\b")
                .replace("\\f", "\u000C")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
            return HJsonString(value)
        }
    }

    val encodedValue: String = value
        .replace("\\", "\\\\")
        .replace("\b", "\\b")
        .replace("\u000C", "\\f")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("\"", "\\\"")


    private val couldBeNumber: Boolean get() = this.value.toDoubleOrNull() != null

    override fun asString(): HJsonString {
        return this
    }

    override fun toJsonString(): String {
        return """"${this.encodedValue}""""
    }

    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toJsonString()
    }

    override fun toHJsonString(indent: String, increment: String): String {
        return when {
            this.value.isEmpty() -> "\"\""
            this.value.contains("\n") -> """'''${this.encodedValue}'''"""
            this.value.contains(Regex("[,:\\[\\]{}]")) -> "\"${this.encodedValue}\""
            this.couldBeNumber -> "\"${this.encodedValue}\""
            else -> this.encodedValue
        }
    }
}

/** A HJSON array. */
class HJsonArray(
    initialElements: List<HJsonValue>
) : HJsonValue() {
    constructor() : this(emptyList())

    var elements: List<HJsonValue> = initialElements.toMutableList()

    override fun asArray(): HJsonArray {
        return this
    }

    override fun toJsonString(): String {
        val elements = this.elements.map {
            it.toJsonString()
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

    override fun toHJsonString(indent: String, increment: String): String {
        return when (elements.size) {
            0 -> "[]"
            else -> {
                val elements = this.elements.map {
                    indent + it.toHJsonString(indent + increment, increment)
                }.joinToString("\n")
                return "[\n${elements}\n${indent.substringBeforeLast(increment)}]"
            }
        }
    }

    /** Appends [element] and returns this array. */
    fun addElement(element: HJsonValue): HJsonArray {
        (this.elements as MutableList).add(element)
        return this
    }

    override fun hashCode(): Int {
        return this.elements.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other is HJsonArray -> this.elements == other.elements
            else -> false
        }
    }

    override fun toString(): String {
        return this.toHJsonString("  ", "  ")
    }
}

/** The HJSON `null` value. */
object HJsonNull : HJsonValue() {
    override fun toJsonString(): String {
        return "null"
    }


    override fun toFormattedJsonString(indent: String, increment: String): String {
        return this.toJsonString()
    }

    override fun toHJsonString(indent: String, increment: String): String {
        return this.toJsonString()
    }

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean {
        return when {
            other is HJsonNull -> true
            else -> false
        }
    }

    override fun toString(): String {
        return "null"
    }
}