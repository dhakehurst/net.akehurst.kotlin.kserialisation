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

/**
 * Builds a [HJsonDocument] using the builder DSL.
 * The block must give exactly one root value, e.g.
 * ```
 * val doc = hjson("example") {
 *     objectReferenceable("com.example.Node") {
 *         property("name", "root")
 *         property("size") { primitiveObject("kotlin.Int", 5) }
 *     }
 * }
 * ```
 */
inline fun hjson(documentIdentity: String, init: HJsonDocumentBuilder.() -> Unit): HJsonDocument {
    val builder = HJsonDocumentBuilder(documentIdentity)
    builder.init()
    return builder.build()
}

/**
 * Converts a Kotlin `Boolean`, `Number`, `String` or enum to a [HJsonValue].
 *
 * @throws HJsonException for any other type
 */
fun Any.toHJsonValue(): HJsonValue {
    return when (this) {
        is Boolean -> HJsonBoolean(this)
        is Number -> HJsonNumber(this.toString())
        is String -> HJsonString(this)
        is Enum<*> -> HJsonString(this.name)
        else -> throw HJsonException("Cannot convert ${this::class.simpleName} to HJsonValue")
    }
}

/** Builder for a [HJsonDocument]; see [hjson]. */
class HJsonDocumentBuilder(documentIdentity: String) {

    private val _doc = HJsonDocument(documentIdentity)
    private val _rootBuilder = HJsonValueBuilder(_doc, emptyList()) { self ->
        if (null == self.value) {
            //ok
        } else {
            throw HJsonException("There can be only one HJsonValue as a the root of a document")
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

    /** Adds a Kotlin `Boolean`, `Number`, `String` or enum as a plain value. */
    fun primitive(value: Any) {
        this._rootBuilder.primitive(value)
    }

    /** Adds a `$PRIMITIVE` object with `$class` [className] and `$value` [value]. */
    fun primitiveObject(className: String, value: Any) {
        this._rootBuilder.primitiveObject(className, value)
    }

    /** Adds an `$ENUM` object with `$class` [className] and `$value` the name of [value]. */
    fun enumObject(className: String, value: Enum<*>) {
        this._rootBuilder.enumObject(className,value)
    }

    /** Adds a plain HJSON array, built by [init]. */
    fun arrayJson(init: HJsonArrayBuilder.() -> Unit) {
        this._rootBuilder.arrayJson(init)
    }

    /** Adds an `$ARRAY` object whose elements are built by [init]. */
    fun arrayObject(init: HJsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.arrayObject(init)
    }

    /** Adds a `$LIST` object whose elements are built by [init]. */
    fun listObject(init: HJsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.listObject(init)
    }

    /** Adds a `$SET` object whose elements are built by [init]. */
    fun setObject(init: HJsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.setObject(init)
    }

    /** Adds a `$MAP` object whose entries are built by [init]. */
    fun mapObject(init: HJsonMapBuilder.() -> Unit) {
        this._rootBuilder.mapObject(init)
    }

    /** Adds a plain HJSON object (that cannot be referenced) whose properties are built by [init]. */
    fun objectJson(init: HJsonObjectBuilder.() -> Unit) {
        this._rootBuilder.objectJson(init)
    }

    /** Adds an `$OBJECT` object of class [className], that can be referenced, whose properties are built by [init]. */
    fun objectReferenceable(className: String, init: HJsonObjectBuilder.() -> Unit) {
        this._rootBuilder.objectReferenceable(className, init)
    }


    /** Returns the value built so far. */
    fun build(): HJsonDocument {
        this._doc.root = _rootBuilder.value ?: throw HJsonException("The document must have a root value")
        return this._doc
    }
}

/** Builder for a plain HJSON array, whose elements are added by calling the functions of this builder in order. */
class HJsonArrayBuilder(
        val doc: HJsonDocument,
        val path: List<String>
) {

    private val _elements = mutableListOf<HJsonValue>()
    /** The path that the next element added will have. */
    val nextPath: List<String>
        get() {
            return this.path + (this._elements.size + 1).toString()
        }

    /** Adds a `null` value. */
    fun nullValue() {
        val b = HJsonValueBuilder(doc, nextPath)
        b.nullValue()
        this._elements.add(b.value!!)
    }

    /** Adds a boolean value. */
    fun boolean(value: Boolean) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.boolean(value)
        this._elements.add(b.value!!)
    }

    /** Adds a number value. */
    fun number(value: Number) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.number(value)
        this._elements.add(b.value!!)
    }

    /** Adds a string value. */
    fun string(value: String) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.string(value)
        this._elements.add(b.value!!)
    }

    /** Adds a Kotlin `Boolean`, `Number`, `String` or enum as a plain value. */
    fun primitive(value: Any) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.primitive(value)
        this._elements.add(b.value!!)
    }

    /** Adds a `$PRIMITIVE` object with `$class` [className] and `$value` [value]. */
    fun primitiveObject(className: String, value: Any) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.primitiveObject(className, value)
        this._elements.add(b.value!!)
    }

    /** Adds an `$ENUM` object with `$class` [className] and `$value` the name of [value]. */
    fun enumObject(className: String, value: Enum<*>) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.enumObject(className, value)
        this._elements.add(b.value!!)
    }

    /** Adds a reference to the object at [path], given as `/`-separated segments starting with `#/`, e.g. `#/a/b` (`/a/b` is also accepted). */
    fun reference(path:String) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.reference(path)
        this._elements.add(b.value!!)
    }

    /** Adds a plain HJSON array, built by [init]. */
    fun arrayJson(init: HJsonArrayBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.arrayJson(init)
        this._elements.add(b.value!!)
    }

    /** Adds an `$ARRAY` object whose elements are built by [init]. */
    fun arrayObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.arrayObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$LIST` object whose elements are built by [init]. */
    fun listObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.listObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$SET` object whose elements are built by [init]. */
    fun setObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.setObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$MAP` object whose entries are built by [init]. */
    fun mapObject(init: HJsonMapBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.mapObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a plain HJSON object (that cannot be referenced) whose properties are built by [init]. */
    fun objectJson(init: HJsonObjectBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.objectJson(init)
        this._elements.add(b.value!!)
    }

    /** Adds an `$OBJECT` object of class [className], that can be referenced, whose properties are built by [init]. */
    fun objectReferenceable(className: String, init: HJsonObjectBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.objectReferenceable(className, init)
        this._elements.add(b.value!!)
    }

    /** Returns the value built so far. */
    fun build(): HJsonArray {
        val json = HJsonArray()
        json.elements = this._elements
        return json
    }
}

/** Builder for the elements of a serialised list, set or array object (`$LIST`, `$SET`, `$ARRAY`). */
class HJsonCollectionBuilder(
        val doc: HJsonDocument,
        val path: List<String>
) {

    private val _elements = mutableListOf<HJsonValue>()
    /** The path that the next element added will have. */
    val nextPath: List<String>
        get() {
            return this.path + (this._elements.size + 1).toString()
        }

    /** Adds a `null` value. */
    fun nullValue() {
        val b = HJsonValueBuilder(doc, nextPath)
        b.nullValue()
        this._elements.add(b.value!!)
    }

    /** Adds a boolean value. */
    fun boolean(value: Boolean) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.boolean(value)
        this._elements.add(b.value!!)
    }

    /** Adds a number value. */
    fun number(value: Number) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.number(value)
        this._elements.add(b.value!!)
    }

    /** Adds a string value. */
    fun string(value: String) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.string(value)
        this._elements.add(b.value!!)
    }

    /** Adds a Kotlin `Boolean`, `Number`, `String` or enum as a plain value. */
    fun primitive(value: Any) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.primitive(value)
        this._elements.add(b.value!!)
    }

    /** Adds a `$PRIMITIVE` object with `$class` [className] and `$value` [value]. */
    fun primitiveObject(className: String, value: Any) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.primitiveObject(className, value)
        this._elements.add(b.value!!)
    }

    /** Adds an `$ENUM` object with `$class` [className] and `$value` the name of [value]. */
    fun enumObject(className: String, value: Enum<*>) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.enumObject(className, value)
        this._elements.add(b.value!!)
    }

    /** Adds a reference to the object at [path], given as `/`-separated segments starting with `#/`, e.g. `#/a/b` (`/a/b` is also accepted). */
    fun reference(path:String) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.reference(path)
        this._elements.add(b.value!!)
    }

    /** Adds a plain HJSON array, built by [init]. */
    fun arrayJson(init: HJsonArrayBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.arrayJson(init)
        this._elements.add(b.value!!)
    }

    /** Adds an `$ARRAY` object whose elements are built by [init]. */
    fun arrayObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.arrayObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$LIST` object whose elements are built by [init]. */
    fun listObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.listObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$SET` object whose elements are built by [init]. */
    fun setObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.setObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a `$MAP` object whose entries are built by [init]. */
    fun mapObject(init: HJsonMapBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.mapObject(init)
        this._elements.add(b.value!!)
    }

    /** Adds a plain HJSON object (that cannot be referenced) whose properties are built by [init]. */
    fun objectJson(init: HJsonObjectBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.objectJson(init)
        this._elements.add(b.value!!)
    }

    /** Adds an `$OBJECT` object of class [className], that can be referenced, whose properties are built by [init]. */
    fun objectReferenceable(className: String, init: HJsonObjectBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.objectReferenceable(className, init)
        this._elements.add(b.value!!)
    }

    /** Returns the value built so far. */
    fun build(type: HJsonString): HJsonUnreferencableObject {
        val obj = HJsonUnreferencableObject()
        obj.setProperty(HJsonDocument.KIND, type)
        val elements = HJsonArray()
        elements.elements = this._elements
        obj.setProperty(HJsonDocument.ELEMENTS, elements)
        return obj
    }
}

/** Builder for the entries of a serialised map object (`$MAP`). */
class HJsonMapBuilder(
        val doc: HJsonDocument,
        val path: List<String>
) {

    private val _entries = mutableListOf<HJsonUnreferencableObject>()

    /** Adds an entry whose key and value are Kotlin `Boolean`, `Number`, `String` or enum values. */
    fun entry(key: Any, value: Any) {
        val jKey = key.toHJsonValue()
        val jValue = value.toHJsonValue()
        val entry = HJsonUnreferencableObject()
        entry.setProperty(HJsonDocument.KEY, jKey)
        entry.setProperty(HJsonDocument.VALUE, jValue)
        this._entries.add(entry)
    }

    /** Adds an entry whose key and value are built by the given builders. */
    fun entry(key: HJsonValueBuilder.() -> Unit, value: HJsonValueBuilder.() -> Unit) {
        val kBuilder = HJsonValueBuilder(doc, path) { self ->
            if (null == self.value) {
                //ok
            } else {
                throw HJsonException("There can be only one JsonValue for a map entry key")
            }
        }
        val vBuilder = HJsonValueBuilder(doc, path) { self ->
            if (null == self.value) {
                //ok
            } else {
                throw HJsonException("There can be only one JsonValue for a map entry value")
            }
        }
        kBuilder.key()
        vBuilder.value()
        val jKey = kBuilder.value ?: throw HJsonException("No value for map entry key")
        val jValue = vBuilder.value ?: throw HJsonException("No value for map entry value")
        val entry = HJsonUnreferencableObject()
        entry.setProperty(HJsonDocument.KEY, jKey)
        entry.setProperty(HJsonDocument.VALUE, jValue)
        this._entries.add(entry)
    }

    /** Returns the value built so far. */
    fun build(): HJsonUnreferencableObject {
        val obj = HJsonUnreferencableObject()
        obj.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.MAP.asHJsonString)
        val elements = HJsonArray()
        elements.elements = _entries
        obj.setProperty(HJsonDocument.ENTRIES, elements)
        return obj
    }
}

/** Builder for the properties of a HJSON object. */
class HJsonObjectBuilder(
        val doc: HJsonDocument,
        val path: List<String>
) {

    private val _properties = mutableMapOf<String, HJsonValue>()

    /** Sets the property [key] to a Kotlin `Boolean`, `Number`, `String`, enum or `null`. */
    fun property(key: String, value: Any?) {
        val jValue = value?.toHJsonValue() ?: HJsonNull
        this._properties[key] = jValue
    }

    /** Sets the property [key] to the value built by [value]. */
    fun property(key: String, value: HJsonValueBuilder.() -> Unit) {
        val vBuilder = HJsonValueBuilder(doc, path + key) { self ->
            if (null == self.value) {
                //ok
            } else {
                throw HJsonException("There can be only one JsonValue for an object property")
            }
        }
        vBuilder.value()
        val jValue = vBuilder.value ?: throw HJsonException("No value for object property")
        this._properties[key] = jValue
    }

    /** Returns the value built so far. */
    fun build(path: List<String>, className: String): HJsonReferencableObject {
        val obj = HJsonReferencableObject(doc, path)
        obj.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.OBJECT.asHJsonString)
        obj.setProperty(HJsonDocument.CLASS, HJsonString(className))
        _properties.forEach {
            obj.setProperty(it.key, it.value)
        }
        return obj
    }

    /** Returns the value built so far. */
    fun build(): HJsonUnreferencableObject {
        val obj = HJsonUnreferencableObject()
        obj.property = _properties
        return obj
    }
}

/** Builder for a single HJSON value. Each function sets the value; it may only be set once. */
class HJsonValueBuilder(
        val doc: HJsonDocument,
        val path: List<String>,
        val validate: (self: HJsonValueBuilder) -> Unit = {}
) {

    var value: HJsonValue? = null

    /** Adds a `null` value. */
    fun nullValue() {
        this.validate(this)
        value = HJsonNull
    }

    /** Adds a boolean value. */
    fun boolean(value: Boolean) {
        this.validate(this)
        this.value = HJsonBoolean(value)
    }

    /** Adds a number value. */
    fun number(value: Number) {
        this.validate(this)
        this.value = HJsonNumber(value.toString())
    }

    /** Adds a string value. */
    fun string(value: String) {
        this.validate(this)
        this.value = HJsonString(value)
    }

    /** Adds a Kotlin `Boolean`, `Number`, `String` or enum as a plain value. */
    fun primitive(value: Any) {
        this.validate(this)
        this.value = value.toHJsonValue()
    }

    /** Adds a `$PRIMITIVE` object with `$class` [className] and `$value` [value]. */
    fun primitiveObject(className: String, value: Any) {
        this.validate(this)
        val obj = HJsonUnreferencableObject()
        obj.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.PRIMITIVE.asHJsonString)
        obj.setProperty(HJsonDocument.CLASS, HJsonString(className))
        obj.setProperty(HJsonDocument.VALUE, value.toHJsonValue())
        this.value = obj
    }

    /** Adds an `$ENUM` object with `$class` [className] and `$value` the name of [value]. */
    fun enumObject(className: String, value: Enum<*>) {
        this.validate(this)
        val obj = HJsonUnreferencableObject()
        obj.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.ENUM.asHJsonString)
        obj.setProperty(HJsonDocument.CLASS, HJsonString(className))
        obj.setProperty(HJsonDocument.VALUE, value.toHJsonValue())
        this.value = obj
    }

    /** Adds a reference to the object at [path], given as `/`-separated segments starting with `#/`, e.g. `#/a/b` (`/a/b` is also accepted). */
    fun reference(path:String) {
        this.validate(this)
        this.value = HJsonReference(doc,path)
    }

    /** Adds a plain HJSON array, built by [init]. */
    fun arrayJson(init: HJsonArrayBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonArrayBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    /** Adds an `$ARRAY` object whose elements are built by [init]. */
    fun arrayObject(init: HJsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(HJsonDocument.ComplexObjectKind.ARRAY.asHJsonString)
    }

    /** Adds a `$LIST` object whose elements are built by [init]. */
    fun listObject(init: HJsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(HJsonDocument.ComplexObjectKind.LIST.asHJsonString)
    }

    /** Adds a `$SET` object whose elements are built by [init]. */
    fun setObject(init: HJsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(HJsonDocument.ComplexObjectKind.SET.asHJsonString)
    }

    /** Adds a `$MAP` object whose entries are built by [init]. */
    fun mapObject(init: HJsonMapBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonMapBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    /** Adds a plain HJSON object (that cannot be referenced) whose properties are built by [init]. */
    fun objectJson(init: HJsonObjectBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonObjectBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    /** Adds an `$OBJECT` object of class [className], that can be referenced, whose properties are built by [init]. */
    fun objectReferenceable(className: String, init: HJsonObjectBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonObjectBuilder(doc, path)
        builder.init()
        this.value = builder.build(path, className)
    }

}