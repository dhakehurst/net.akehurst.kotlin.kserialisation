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

@DslMarker
annotation class JsonDocumentMarker

inline fun json(documentIdentity: String, init: JsonDocumentBuilder.() -> Unit): JsonDocument {
    val builder = JsonDocumentBuilder(documentIdentity)
    builder.init()
    return builder.build()
}

fun Any.toJsonValue(): JsonValue {
    return when (this) {
        is Boolean -> JsonBoolean(this)
        is Number -> JsonNumber(this.toString())
        is String -> JsonString(this)
        is Enum<*> -> JsonString(this.name)
        else -> throw JsonException("Cannot convert ${this::class} to JsonValue")
    }
}

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

    fun nullValue() {
        this._rootBuilder.nullValue()
    }

    fun boolean(value: Boolean) {
        this._rootBuilder.boolean(value)
    }

    fun number(value: Number) {
        this._rootBuilder.number(value)
    }

    fun string(value: String) {
        this._rootBuilder.string(value)
    }

    fun primitiveObject(className: String, value: Any) {
        this._rootBuilder.primitiveObject(className, value)
    }

    fun enumObject(className: String, value: Enum<*>) {
        this._rootBuilder.enumObject(className,value)
    }

    fun arrayJson(init: JsonArrayBuilder.() -> Unit) {
        this._rootBuilder.arrayJson(init)
    }

    fun arrayObject(init: JsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.arrayObject(init)
    }

    fun listObject(init: JsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.listObject(init)
    }

    fun setObject(init: JsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.setObject(init)
    }

    fun mapObject(init: JsonMapBuilder.() -> Unit) {
        this._rootBuilder.mapObject(init)
    }

    fun objectJson(init: JsonObjectBuilder.() -> Unit) {
        this._rootBuilder.objectJson(init)
    }

    fun objectReferenceable(className: String, init: JsonObjectBuilder.() -> Unit) {
        this._rootBuilder.objectReferenceable(className, init)
    }


    fun build(): JsonDocument {
        this._doc.root = _rootBuilder.value ?: throw JsonException("The document must have a root value")
        return this._doc
    }
}

@JsonDocumentMarker
class JsonArrayBuilder(
        val doc: JsonDocument,
        val path: List<String>
) {

    private val _elements = mutableListOf<JsonValue>()
    val nextPath: List<String>
        get() {
            return this.path + (this._elements.size + 1).toString()
        }

    fun nullValue() {
        val b = JsonValueBuilder(doc, nextPath)
        b.nullValue()
        this._elements.add(b.value!!)
    }

    fun boolean(value: Boolean) {
        val b = JsonValueBuilder(doc, nextPath)
        b.boolean(value)
        this._elements.add(b.value!!)
    }

    fun number(value: Number) {
        val b = JsonValueBuilder(doc, nextPath)
        b.number(value)
        this._elements.add(b.value!!)
    }

    fun string(value: String) {
        val b = JsonValueBuilder(doc, nextPath)
        b.string(value)
        this._elements.add(b.value!!)
    }

    fun primitiveObject(className: String, value: Any) {
        val b = JsonValueBuilder(doc, nextPath)
        b.primitiveObject(className, value)
        this._elements.add(b.value!!)
    }

    fun enumObject(className: String, value: Enum<*>) {
        val b = JsonValueBuilder(doc, nextPath)
        b.enumObject(className, value)
        this._elements.add(b.value!!)
    }

    fun reference(path:String) {
        val b = JsonValueBuilder(doc, nextPath)
        b.reference(path)
        this._elements.add(b.value!!)
    }

    fun arrayJson(init: JsonArrayBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.arrayJson(init)
        this._elements.add(b.value!!)
    }

    fun arrayObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.arrayObject(init)
        this._elements.add(b.value!!)
    }

    fun listObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.listObject(init)
        this._elements.add(b.value!!)
    }

    fun setObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.setObject(init)
        this._elements.add(b.value!!)
    }

    fun mapObject(init: JsonMapBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.mapObject(init)
        this._elements.add(b.value!!)
    }

    fun objectJson(init: JsonObjectBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.objectJson(init)
        this._elements.add(b.value!!)
    }

    fun objectReferenceable(className: String, init: JsonObjectBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.objectReferenceable(className, init)
        this._elements.add(b.value!!)
    }

    fun build(): JsonArray {
        val json = JsonArray()
        json.elements = this._elements
        return json
    }
}

@JsonDocumentMarker
class JsonCollectionBuilder(
        val doc: JsonDocument,
        val path: List<String>
) {

    private val _elements = mutableListOf<JsonValue>()
    val nextPath: List<String>
        get() {
            return this.path + (this._elements.size + 1).toString()
        }

    fun nullValue() {
        val b = JsonValueBuilder(doc, nextPath)
        b.nullValue()
        this._elements.add(b.value!!)
    }

    fun boolean(value: Boolean) {
        val b = JsonValueBuilder(doc, nextPath)
        b.boolean(value)
        this._elements.add(b.value!!)
    }

    fun number(value: Number) {
        val b = JsonValueBuilder(doc, nextPath)
        b.number(value)
        this._elements.add(b.value!!)
    }

    fun string(value: String) {
        val b = JsonValueBuilder(doc, nextPath)
        b.string(value)
        this._elements.add(b.value!!)
    }

    fun primitiveObject(className: String, value: Any) {
        val b = JsonValueBuilder(doc, nextPath)
        b.primitiveObject(className, value)
        this._elements.add(b.value!!)
    }

    fun enumObject(className: String, value: Enum<*>) {
        val b = JsonValueBuilder(doc, nextPath)
        b.enumObject(className, value)
        this._elements.add(b.value!!)
    }

    fun reference(path:String) {
        val b = JsonValueBuilder(doc, nextPath)
        b.reference(path)
        this._elements.add(b.value!!)
    }

    fun arrayJson(init: JsonArrayBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.arrayJson(init)
        this._elements.add(b.value!!)
    }

    fun arrayObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.arrayObject(init)
        this._elements.add(b.value!!)
    }

    fun listObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.listObject(init)
        this._elements.add(b.value!!)
    }

    fun setObject(init: JsonCollectionBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.setObject(init)
        this._elements.add(b.value!!)
    }

    fun mapObject(init: JsonMapBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.mapObject(init)
        this._elements.add(b.value!!)
    }

    fun objectJson(init: JsonObjectBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.objectJson(init)
        this._elements.add(b.value!!)
    }

    fun objectReferenceable(className: String, init: JsonObjectBuilder.() -> Unit) {
        val b = JsonValueBuilder(doc, nextPath)
        b.objectReferenceable(className, init)
        this._elements.add(b.value!!)
    }

    fun build(type: JsonString): JsonUnreferencableObject {
        val obj = JsonUnreferencableObject()
        obj.setProperty(JsonDocument.KIND, type)
        val elements = JsonArray()
        elements.elements = this._elements
        obj.setProperty(JsonDocument.ELEMENTS, elements)
        return obj
    }
}

@JsonDocumentMarker
class JsonMapBuilder(
        val doc: JsonDocument,
        val path: List<String>
) {

    private val _entries = mutableListOf<JsonUnreferencableObject>()

    fun entry(key: Any, value: Any) {
        val jKey = key.toJsonValue()
        val jValue = value.toJsonValue()
        val entry = JsonUnreferencableObject()
        entry.setProperty(JsonDocument.KEY, jKey)
        entry.setProperty(JsonDocument.VALUE, jValue)
        this._entries.add(entry)
    }

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

    fun build(): JsonUnreferencableObject {
        val obj = JsonUnreferencableObject()
        obj.setProperty(JsonDocument.KIND, JsonDocument.ComplexObjectKind.MAP.asJsonString)
        val elements = JsonArray()
        elements.elements = _entries
        obj.setProperty(JsonDocument.ENTRIES, elements)
        return obj
    }
}

@JsonDocumentMarker
class JsonObjectBuilder(
        val doc: JsonDocument,
        val path: List<String>
) {

    private val _properties = mutableMapOf<String, JsonValue>()

    fun property(key: String, value: Any?) {
        val jValue = value?.toJsonValue() ?: JsonNull
        this._properties[key] = jValue
    }

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

    fun build(path: List<String>, className: String): JsonReferencableObject {
        val obj = JsonReferencableObject(doc, path)
        obj.setProperty(JsonDocument.KIND, JsonDocument.ComplexObjectKind.OBJECT.asJsonString)
        obj.setProperty(JsonDocument.CLASS, JsonString(className))
        _properties.forEach {
            obj.setProperty(it.key, it.value)
        }
        return obj
    }

    fun build(): JsonUnreferencableObject {
        val obj = JsonUnreferencableObject()
        obj.property = _properties
        return obj
    }
}

@JsonDocumentMarker
class JsonValueBuilder(
        val doc: JsonDocument,
        val path: List<String>,
        val validate: (self: JsonValueBuilder) -> Unit = {}
) {

    var value: JsonValue? = null

    fun nullValue() {
        this.validate(this)
        value = JsonNull
    }

    fun boolean(value: Boolean) {
        this.validate(this)
        this.value = JsonBoolean(value)
    }

    fun number(value: Number) {
        this.validate(this)
        this.value = JsonNumber(value.toString())
    }

    fun string(rawValue: String) {
        this.validate(this)
        this.value = JsonString(rawValue)
    }

    fun primitiveObject(className: String, value: Any) {
        this.validate(this)
        val obj = JsonUnreferencableObject()
        obj.setProperty(JsonDocument.KIND, JsonDocument.ComplexObjectKind.PRIMITIVE.asJsonString)
        obj.setProperty(JsonDocument.CLASS, JsonString(className))
        obj.setProperty(JsonDocument.VALUE, value.toJsonValue())
        this.value = obj
    }

    fun enumObject(className: String, value: Enum<*>) {
        this.validate(this)
        val obj = JsonUnreferencableObject()
        obj.setProperty(JsonDocument.KIND, JsonDocument.ComplexObjectKind.ENUM.asJsonString)
        obj.setProperty(JsonDocument.CLASS, JsonString(className))
        obj.setProperty(JsonDocument.VALUE, value.toJsonValue())
        this.value = obj
    }

    fun reference(path:String) {
        this.validate(this)
        // remove leading '/' then split
        val refPath = path.substring(1).split("/").toList()
        this.value = JsonReference(doc,refPath)
    }

    fun arrayJson(init: JsonArrayBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonArrayBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    fun arrayObject(init: JsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(JsonDocument.ComplexObjectKind.ARRAY.asJsonString)
    }

    fun listObject(init: JsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(JsonDocument.ComplexObjectKind.LIST.asJsonString)
    }

    fun setObject(init: JsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(JsonDocument.ComplexObjectKind.SET.asJsonString)
    }

    fun mapObject(init: JsonMapBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonMapBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    fun objectJson(init: JsonObjectBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonObjectBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    fun objectReferenceable(className: String, init: JsonObjectBuilder.() -> Unit) {
        this.validate(this)
        val builder = JsonObjectBuilder(doc, path)
        builder.init()
        this.value = builder.build(path, className)
    }

}