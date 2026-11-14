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

/** DSL marker for the JSON builder DSL. */
@DslMarker
annotation class JsonDocumentMarker

/**
 * Builds a [JsonDocument] using the builder DSL.
 * The block must give exactly one root value, e.g.
 * ```
 * val doc = json("example") {
 *     objectReferenceable("com.example.Node") {
 *         property("name", "root")
 *         property("size") { primitiveObject("kotlin.Int", 5) }
 *     }
 * }
 * ```
 */
inline fun json(documentIdentity: String, init: JsonDocumentBuilder.() -> Unit): JsonDocument {
    val builder = JsonDocumentBuilder(documentIdentity)
    builder.init()
    return builder.build()
}

/**
 * Converts a Kotlin `Boolean`, `Number`, `String` or enum to a [JsonValue].
 *
 * @throws JsonException for any other type
 */
fun Any.toJsonValue(): JsonValue {
    return when (this) {
        is Boolean -> JsonBoolean(this)
        is Number -> JsonNumber(this.toString())
        is String -> JsonString(this)
        is Enum<*> -> JsonString(this.name)
        else -> throw JsonException("Cannot convert ${this::class} to JsonValue")
    }
}

/** Builder for a [JsonDocument]; see [json]. */
@JsonDocumentMarker
class JsonDocumentBuilder(documentIdentity: String) {

    private val _doc = JsonDocument(documentIdentity)
    private val _rootBuilder = JsonValueBuilder(_doc, emptyList()) { self ->
        if (null == self.value) {
            //ok
        } else {
            throw JsonException("There can be only one JsonValue as a the root of a document")
        }
    }

    /** Adds a `null` value. */
    fun nullValue() {
        this._rootBuilder.nullValue()
    }

    /** Adds a boolean value. */
    fun boolean(value: Boolean) {
        this._rootBuilder.boolean(value)
    }

    /** Adds a number value. */
    fun number(value: Number) {
        this._rootBuilder.number(value)
    }

    /** Adds a string value. */
    fun string(value: String) {
        this._rootBuilder.string(value)
    }

    /** Adds a `$PRIMITIVE` object with `$class` [className] and `$value` [value]. */
    fun primitiveObject(className: String, value: Any) {
        this._rootBuilder.primitiveObject(className, value)
    }

    /** Adds an `$ENUM` object with `$class` [className] and `$value` the name of [value]. */
    fun enumObject(className: String, value: Enum<*>) {
        this._rootBuilder.enumObject(className,value)
    }

    /** Adds a plain JSON array, built by [init]. */
    fun arrayJson(init: JsonArrayBuilder.() -> Unit) {
        this._rootBuilder.arrayJson(init)
    }

    /** Adds an `$ARRAY` object whose elements are built by [init]. */
    fun arrayObject(init: JsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.arrayObject(init)
    }

    /** Adds a `$LIST` object whose elements are built by [init]. */
    fun listObject(init: JsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.listObject(init)
    }

    /** Adds a `$SET` object whose elements are built by [init]. */
    fun setObject(init: JsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.setObject(init)
    }

    /** Adds a `$MAP` object whose entries are built by [init]. */
    fun mapObject(init: JsonMapBuilder.() -> Unit) {
        this._rootBuilder.mapObject(init)
    }

    /** Adds a plain JSON object (that cannot be referenced) whose properties are built by [init]. */
    fun objectJson(init: JsonObjectBuilder.() -> Unit) {
        this._rootBuilder.objectJson(init)
    }

    /** Adds an `$OBJECT` object of class [className], that can be referenced, whose properties are built by [init]. */
    fun objectReferenceable(className: String, init: JsonObjectBuilder.() -> Unit) {
        this._rootBuilder.objectReferenceable(className, init)
    }


    /** Returns the value built so far. */
    fun build(): JsonDocument {
        this._doc.root = _rootBuilder.value ?: throw JsonException("The document must have a root value")
        return this._doc
    }
}

/** Builder for a plain JSON array, whose elements are added by calling the functions of this builder in order. */
@JsonDocumentMarker
class JsonArrayBuilder(
        val doc: JsonDocument,
        val path: List<String>
) {

    private val _elements = mutableListOf<JsonValue>()
    /** The path that the next element added will have. */
    val nextPath: List<String>
        get() {
            return this.path + (this._elements.size + 1).toString()
        }

    /** Adds a `null` value. */
    fun nullValue() {
        val b = JsonValueBuilder(doc, nextPath)
        b.nullValue()
        this._elements.add(b.value!!)
    }

    /** Adds a boolean value. */
    fun boolean(value: Boolean) {
        val b = JsonValueBuilder(doc, nextPath)
        b.boolean(value)
        this._elements.add(b.value!!)
    }

    /** Adds a number value. */
    fun number(value: Number) {
        val b = JsonValueBuilder(doc, nextPath)
        b.number(value)
        this._elements.add(b.value!!)
    }

    /** Adds a string value. */
    fun string(value: String) {
        val b = JsonValueBuilder(doc, nextPath)
        b.string(value)
        this._elements.add(b.value!!)
    }

    /** Adds a `$PRIMITIVE` object with `$class` [className] and `$value` [value]. */
    fun primitiveObject(className: String, value: Any) {
        val b = JsonValueBuilder(doc, nextPath)
        b.primitiveObject(className, value)
        this._elements.add(b.value!!)
    }

    /** Adds an `$ENUM` object with `$class` [className] and `$value` the name of [value]. */
    fun enumObject(className: String, value: Enum<*>) {
        val b = JsonValueBuilder(doc, nextPath)
        b.enumObject(className, value)
        this._elements.add(b.value!!)
    }

    /** Adds a reference to the object at [path], given as `/`-separated segments starting with `#/`, e.g. `#/a/b` (`/a/b` is also accepted). */
    fun reference(path:String) {
        val b = JsonValueBuilder(doc, nextPath)
        b.reference(path)
        this._elements.add(b.value!!)
    }

    /** Adds a plain JSON array, built by [init]. */
    fun arrayJson(init: JsonArrayBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.arrayJson(init)
        this._elements.add(b.value!!)
    }

    /** Adds an `$ARRAY` object whose elements are built by [init]. */
    fun arrayObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.arrayObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$LIST` object whose elements are built by [init]. */
    fun listObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.listObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$SET` object whose elements are built by [init]. */
    fun setObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.setObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$MAP` object whose entries are built by [init]. */
    fun mapObject(init: JsonMapBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.mapObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a plain JSON object (that cannot be referenced) whose properties are built by [init]. */
    fun objectJson(init: JsonObjectBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.objectJson(init)
        this._elements.add(b.value!!)
    }

    /** Adds an `$OBJECT` object of class [className], that can be referenced, whose properties are built by [init]. */
    fun objectReferenceable(className: String, init: JsonObjectBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.objectReferenceable(className, init)
        this._elements.add(b.value!!)
    }

    /** Returns the value built so far. */
    fun build(): JsonArray {
        val json = JsonArray()
        json.elements = this._elements
        return json
    }
}

/** Builder for the elements of a serialised list, set or array object (`$LIST`, `$SET`, `$ARRAY`). */
@JsonDocumentMarker
class JsonCollectionBuilder(
        val doc: JsonDocument,
        val path: List<String>
) {

    private val _elements = mutableListOf<JsonValue>()
    /** The path that the next element added will have. */
    val nextPath: List<String>
        get() {
            return this.path + (this._elements.size + 1).toString()
        }

    /** Adds a `null` value. */
    fun nullValue() {
        val b = JsonValueBuilder(doc, nextPath)
        b.nullValue()
        this._elements.add(b.value!!)
    }

    /** Adds a boolean value. */
    fun boolean(value: Boolean) {
        val b = JsonValueBuilder(doc, nextPath)
        b.boolean(value)
        this._elements.add(b.value!!)
    }

    /** Adds a number value. */
    fun number(value: Number) {
        val b = JsonValueBuilder(doc, nextPath)
        b.number(value)
        this._elements.add(b.value!!)
    }

    /** Adds a string value. */
    fun string(value: String) {
        val b = JsonValueBuilder(doc, nextPath)
        b.string(value)
        this._elements.add(b.value!!)
    }

    /** Adds a `$PRIMITIVE` object with `$class` [className] and `$value` [value]. */
    fun primitiveObject(className: String, value: Any) {
        val b = JsonValueBuilder(doc, nextPath)
        b.primitiveObject(className, value)
        this._elements.add(b.value!!)
    }

    /** Adds an `$ENUM` object with `$class` [className] and `$value` the name of [value]. */
    fun enumObject(className: String, value: Enum<*>) {
        val b = JsonValueBuilder(doc, nextPath)
        b.enumObject(className, value)
        this._elements.add(b.value!!)
    }

    /** Adds a reference to the object at [path], given as `/`-separated segments starting with `#/`, e.g. `#/a/b` (`/a/b` is also accepted). */
    fun reference(path:String) {
        val b = JsonValueBuilder(doc, nextPath)
        b.reference(path)
        this._elements.add(b.value!!)
    }

    /** Adds a plain JSON array, built by [init]. */
    fun arrayJson(init: JsonArrayBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.arrayJson(init)
        this._elements.add(b.value!!)
    }

    /** Adds an `$ARRAY` object whose elements are built by [init]. */
    fun arrayObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.arrayObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$LIST` object whose elements are built by [init]. */
    fun listObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.listObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$SET` object whose elements are built by [init]. */
    fun setObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.setObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$MAP` object whose entries are built by [init]. */
    fun mapObject(init: JsonMapBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.mapObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a plain JSON object (that cannot be referenced) whose properties are built by [init]. */
    fun objectJson(init: JsonObjectBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.objectJson(init)
        this._elements.add(b.value!!)
    }

    /** Adds an `$OBJECT` object of class [className], that can be referenced, whose properties are built by [init]. */
    fun objectReferenceable(className: String, init: JsonObjectBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.objectReferenceable(className, init)
        this._elements.add(b.value!!)
    }

    /** Returns the value built so far. */
    fun build(type: JsonString): JsonUnreferencableObject {
        val obj = JsonUnreferencableObject()
        obj.setProperty(JsonDocument.KIND, type)
        val elements = JsonArray()
        elements.elements = this._elements
        obj.setProperty(JsonDocument.ELEMENTS, elements)
        return obj
    }
}

/** Builder for the entries of a serialised map object (`$MAP`). */
@JsonDocumentMarker
class JsonMapBuilder(
        val doc: JsonDocument,
        val path: List<String>
) {

    private val _entries = mutableListOf<JsonUnreferencableObject>()

    /** Adds an entry whose key and value are Kotlin `Boolean`, `Number`, `String` or enum values. */
    fun entry(key: Any, value: Any) {
        val jKey = key.toJsonValue()
        val jValue = value.toJsonValue()
        val entry = JsonUnreferencableObject()
        entry.setProperty(JsonDocument.KEY, jKey)
        entry.setProperty(JsonDocument.VALUE, jValue)
        this._entries.add(entry)
    }

    /** Adds an entry whose key and value are built by the given builders. */
    fun entry(key: JsonValueBuilder.() -> Unit, value: JsonValueBuilder.() -> Unit) {
        val kBuilder = JsonValueBuilder(doc, path) { self ->
            if (null == self.value) {
                //ok
            } else {
                throw JsonException("There can be only one JsonValue for a map entry key")
            }
        }
        val vBuilder = JsonValueBuilder(doc, path) { self ->
            if (null == self.value) {
                //ok
            } else {
                throw JsonException("There can be only one JsonValue for a map entry value")
            }
        }
        kBuilder.key()
        vBuilder.value()
        val jKey = kBuilder.value ?: throw JsonException("No value for map entry key")
        val jValue = vBuilder.value ?: throw JsonException("No value for map entry value")
        val entry = JsonUnreferencableObject()
        entry.setProperty(JsonDocument.KEY, jKey)
        entry.setProperty(JsonDocument.VALUE, jValue)
        this._entries.add(entry)
    }

    /** Returns the value built so far. */
    fun build(): JsonUnreferencableObject {
        val obj = JsonUnreferencableObject()
        obj.setProperty(JsonDocument.KIND, JsonDocument.ComplexObjectKind.MAP.asJsonString)
        val elements = JsonArray()
        elements.elements = _entries
        obj.setProperty(JsonDocument.ENTRIES, elements)
        return obj
    }
}

/** Builder for the properties of a JSON object. */
@JsonDocumentMarker
class JsonObjectBuilder(
        val doc: JsonDocument,
        val path: List<String>
) {

    private val _properties = mutableMapOf<String, JsonValue>()

    /** Sets the property [key] to a Kotlin `Boolean`, `Number`, `String`, enum or `null`. */
    fun property(key: String, value: Any?) {
        val jValue = value?.toJsonValue() ?: JsonNull
        this._properties[key] = jValue
    }

    /** Sets the property [key] to the value built by [value]. */
    fun property(key: String, value: JsonValueBuilder.() -> Unit) {
        val vBuilder = JsonValueBuilder(doc, path + key) { self ->
            if (null == self.value) {
                //ok
            } else {
                throw JsonException("There can be only one JsonValue for an object property")
            }
        }
        vBuilder.value()
        val jValue = vBuilder.value ?: throw JsonException("No value for object property")
        this._properties[key] = jValue
    }

    /** Returns the value built so far. */
    fun build(path: List<String>, className: String): JsonReferencableObject {
        val obj = JsonReferencableObject(doc, path)
        obj.setProperty(JsonDocument.KIND, JsonDocument.ComplexObjectKind.OBJECT.asJsonString)
        obj.setProperty(JsonDocument.CLASS, JsonString(className))
        _properties.forEach {
            obj.setProperty(it.key, it.value)
        }
        return obj
    }

    /** Returns the value built so far. */
    fun build(): JsonUnreferencableObject {
        val obj = JsonUnreferencableObject()
        obj.property = _properties
        return obj
    }
}

/** Builder for a single JSON value. Each function sets the value; it may only be set once. */
@JsonDocumentMarker
class JsonValueBuilder(
        val doc: JsonDocument,
        val path: List<String>,
        val validate: (self: JsonValueBuilder) -> Unit = {}
) {

    var value: JsonValue? = null

    /** Adds a `null` value. */
    fun nullValue() {
        this.validate(this)
        value = JsonNull
    }

    /** Adds a boolean value. */
    fun boolean(value: Boolean) {
        this.validate(this)
        this.value = JsonBoolean(value)
    }

    /** Adds a number value. */
    fun number(value: Number) {
        this.validate(this)
        this.value = JsonNumber(value.toString())
    }

    /** Adds a string value. */
    fun string(rawValue: String) {
        this.validate(this)
        this.value = JsonString(rawValue)
    }

    /** Adds a `$PRIMITIVE` object with `$class` [className] and `$value` [value]. */
    fun primitiveObject(className: String, value: Any) {
        this.validate(this)
        val obj = JsonUnreferencableObject()
        obj.setProperty(JsonDocument.KIND, JsonDocument.ComplexObjectKind.PRIMITIVE.asJsonString)
        obj.setProperty(JsonDocument.CLASS, JsonString(className))
        obj.setProperty(JsonDocument.VALUE, value.toJsonValue())
        this.value = obj
    }

    /** Adds an `$ENUM` object with `$class` [className] and `$value` the name of [value]. */
    fun enumObject(className: String, value: Enum<*>) {
        this.validate(this)
        val obj = JsonUnreferencableObject()
        obj.setProperty(JsonDocument.KIND, JsonDocument.ComplexObjectKind.ENUM.asJsonString)
        obj.setProperty(JsonDocument.CLASS, JsonString(className))
        obj.setProperty(JsonDocument.VALUE, value.toJsonValue())
        this.value = obj
    }

    /** Adds a reference to the object at [path], given as `/`-separated segments starting with `#/`, e.g. `#/a/b` (`/a/b` is also accepted). */
    fun reference(path:String) {
        this.validate(this)
        this.value = JsonReference(doc, path)
    }

    /** Adds a plain JSON array, built by [init]. */
    fun arrayJson(init: JsonArrayBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonArrayBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    /** Adds an `$ARRAY` object whose elements are built by [init]. */
    fun arrayObject(init: JsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(JsonDocument.ComplexObjectKind.ARRAY.asJsonString)
    }

    /** Adds a `$LIST` object whose elements are built by [init]. */
    fun listObject(init: JsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(JsonDocument.ComplexObjectKind.LIST.asJsonString)
    }

    /** Adds a `$SET` object whose elements are built by [init]. */
    fun setObject(init: JsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(JsonDocument.ComplexObjectKind.SET.asJsonString)
    }

    /** Adds a `$MAP` object whose entries are built by [init]. */
    fun mapObject(init: JsonMapBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonMapBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    /** Adds a plain JSON object (that cannot be referenced) whose properties are built by [init]. */
    fun objectJson(init: JsonObjectBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonObjectBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    /** Adds an `$OBJECT` object of class [className], that can be referenced, whose properties are built by [init]. */
    fun objectReferenceable(className: String, init: JsonObjectBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonObjectBuilder(doc, path)
        builder.init()
        this.value = builder.build(path, className)
    }

}