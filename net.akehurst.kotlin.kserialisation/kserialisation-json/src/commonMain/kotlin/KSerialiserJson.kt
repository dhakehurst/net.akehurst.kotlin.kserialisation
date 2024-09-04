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

import net.akehurst.kotlin.json.*
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.DatatypeRegistry.Companion.isKotlinArray
import net.akehurst.kotlin.komposite.common.DatatypeRegistry.Companion.isKotlinList
import net.akehurst.kotlin.komposite.common.DatatypeRegistry.Companion.isKotlinSet
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.reflect.KClass

class KSerialiserJsonException : RuntimeException {
    constructor(message: String) : super(message)
}

class KSerialiserJson() {

    internal val reference_cache = mutableMapOf<Any, List<String>>()

    val registry = DatatypeRegistry()

    class FoundReferenceException : RuntimeException {
        constructor() : super()
    }

    protected fun calcReferencePath(root: Any, targetValue: Any): List<String> {
        return if (reference_cache.containsKey(targetValue)) {
            reference_cache[targetValue]!!
        } else {
            var resultPath: List<String>? = null //TODO: terminate walking early if result found
            val walker = kompositeWalker<List<String>, Boolean>(registry) {
                singleton { path, info, obj, datatype ->
                    reference_cache[obj] = path
                    if (obj == targetValue) {
                        resultPath = path
                        throw FoundReferenceException()
                        // TODO: find a way to terminate the walk!
                    }
                    WalkInfo(info.up, obj == targetValue)
                }
                collBegin { path, info, type, coll ->
                    WalkInfo(info.up, info.acc)
                }
                mapBegin { path, info, map ->
                    WalkInfo(info.up, info.acc)
                }
                //               mapEntryValueBegin { key, info, entry ->
                //                   val path = if (key== KompositeWalker.ROOT) info.path else info.path + key.toString()
                //                   WalkInfo(path, info.acc)
                //               }
                objectBegin { path, info, obj, datatype ->
                    reference_cache[obj] = path
                    if (obj == targetValue) {
                        resultPath = path
                        throw FoundReferenceException()
                        // TODO: find a way to terminate the walk!
                    }
                    WalkInfo(info.up, obj == targetValue)
                }
                propertyBegin { path, info, property ->
                    WalkInfo(info.up, info.acc)
                }
            }

            try {
                val result = walker.walk(WalkInfo(emptyList(), false), root)
            } catch (e: FoundReferenceException) {

            }
            resultPath ?: listOf("${'$'}unknown ${targetValue::class.simpleName}")
        }
    }

    fun configureFromKompositeString(kompositeModel: String) {
        //TODO: mappers!
        registry.registerFromConfigString(kompositeModel, emptyMap())
    }
    fun configureFromTypeModel(typeModel: TypeModel) {
        //TODO: mappers!
        registry.registerFromTypeModel(typeModel, emptyMap())
    }

    fun registerKotlinStdPrimitives() {
        this.registry.registerFromTypeModel(DatatypeRegistry.KOTLIN_STD_MODEL, emptyMap())
        this.registerPrimitive(Boolean::class, { value -> JsonBoolean(value) }, { json -> json.asBoolean().value })
        this.registerPrimitiveAsObject(Byte::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toByte() })
        this.registerPrimitiveAsObject(Short::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toShort() })
        this.registerPrimitiveAsObject(Int::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toInt() })
        this.registerPrimitiveAsObject(Long::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toLong() })
        this.registerPrimitiveAsObject(Float::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toFloat() })
        this.registerPrimitiveAsObject(Double::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toDouble() })
        this.registerPrimitive(String::class, { value -> JsonString(value) }, { json -> json.asString().value })
    }

    fun <T : Any> registerPrimitive(cls: KClass<T>, toJson: (value: T) -> JsonValue, toPrimitive: (json: JsonValue) -> T) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findTypeDeclarationByKClass(cls) ?: throw KSerialiserJsonException("The primitive is not defined in the Komposite configuration")
        this.registry.registerPrimitiveMapper(PrimitiveMapper.create(cls, JsonValue::class, toJson, toPrimitive))
    }

    fun <P : Any> registerPrimitiveAsObject(cls: KClass<P>, toJson: (value: P) -> JsonValue, fromJson: (json: JsonValue) -> P) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findTypeDeclarationByKClass(cls) ?: throw KSerialiserJsonException("The primitive is not defined in the Komposite configuration")
        val toJsonMpr = { value: P ->
            val obj = JsonUnreferencableObject()
            obj.setProperty(JsonDocument.TYPE, JsonDocument.ComplexObjectKind.PRIMITIVE.asJsonString)
            obj.setProperty(JsonDocument.CLASS, JsonString(dt.qualifiedName.value))
            obj.setProperty(JsonDocument.VALUE, toJson(value))
            obj
        }
        val toPrimitive = { json: JsonObject ->
            val jsonValue = json.property[JsonDocument.VALUE]!!
            fromJson(jsonValue)
        }
        val mapper = PrimitiveMapper.create(cls, JsonObject::class, toJsonMpr, toPrimitive)
        this.registry.registerPrimitiveMapper(mapper)
    }

    fun toJson(root: Any, data: Any): JsonDocument {
        this.reference_cache.clear()
        val doc = JsonDocument("json")
        var currentObjStack = Stack<JsonValue>()
        val walker = kompositeWalker<List<String>, JsonValue>(registry) {
            configure {
                ELEMENTS = JsonDocument.ELEMENTS
                ENTRIES = JsonDocument.ENTRIES
                KEY = JsonDocument.KEY
                VALUE = JsonDocument.VALUE
            }
            nullValue { path, info ->
                WalkInfo(info.up, JsonNull)
            }
            primitive { path, info, primitive, mapper ->
                //TODO: use qualified name when we can!
                val clsName = SimpleName(primitive::class.simpleName!!)
                val dt = registry.findFirstByNameOrNull(clsName)
                    ?: error("The primitive '${clsName}' is not defined in the Komposite configuration")
                val json = (mapper as PrimitiveMapper<Any, JsonValue>?)?.toRaw?.invoke(primitive)
                    ?: error("Do not know how to convert '${clsName}' to json, did you register its converter")
                WalkInfo(info.up, json)
            }
            enum { path, info, enum ->
                val clsName = SimpleName(enum::class.simpleName!!)
                val dt = registry.findFirstByNameOrNull(clsName)
                    ?: error("The enum '${clsName}' is not defined in the Komposite configuration")
                val value = JsonString(enum.name)

                val obj = JsonUnreferencableObject()
                obj.setProperty(JsonDocument.TYPE, JsonDocument.ComplexObjectKind.ENUM.asJsonString)
                obj.setProperty(JsonDocument.CLASS, JsonString(dt.qualifiedName.value))
                obj.setProperty(JsonDocument.VALUE, value)
                WalkInfo(info.up, obj)
            }
            reference { path, info, value, property ->
                val refPath = calcReferencePath(root, value)
                val ref = JsonReference(doc, refPath)
                WalkInfo(path, ref)
            }
            singleton { path, info, obj, datatype ->
                val json = JsonReferencableObject(doc, path)
                reference_cache[obj] = json.path
                json.setProperty(JsonDocument.TYPE, JsonDocument.ComplexObjectKind.OBJECT.asJsonString)
                json.setProperty(JsonDocument.CLASS, JsonString(datatype.qualifiedName.value))
                WalkInfo(path, json)
            }
            collBegin { path, info, type, coll ->
                val elements = JsonArray()
                currentObjStack.push(elements)
                WalkInfo(info.up, elements)
            }
            collElementEnd { path, info, element ->
                val elements = currentObjStack.peek() as JsonArray
                elements.addElement(info.acc)
                //val nObj = listObj.withProperty(ELEMENTS, newList)
                //currentObjStack.push(nObj)
                WalkInfo(info.up, elements)
            }
            collEnd { path, info, type, coll ->
                val jsonTypeName = when {
                    type.isKotlinArray -> JsonDocument.ComplexObjectKind.ARRAY.asJsonString
                    type.isKotlinList -> JsonDocument.ComplexObjectKind.LIST.asJsonString
                    type.isKotlinSet -> JsonDocument.ComplexObjectKind.SET.asJsonString
                    else -> throw KSerialiserJsonException("Unknown type $type")
                }
                val elements = currentObjStack.pop()
                val setObj = JsonUnreferencableObject()
                setObj.setProperty(JsonDocument.TYPE, jsonTypeName)
                //ELEMENT_TYPE to JsonString(type.elementType.qualifiedName), //needed for deserialising empty Arrays
                setObj.setProperty(JsonDocument.ELEMENTS, elements)
                WalkInfo(info.up, setObj)
            }
            mapBegin { path, info, map ->
                val obj = JsonUnreferencableObject()
                obj.setProperty(JsonDocument.TYPE, JsonDocument.ComplexObjectKind.MAP.asJsonString)
                obj.setProperty(JsonDocument.ENTRIES, JsonArray())
                currentObjStack.push(obj)
                WalkInfo(info.up, obj)
            }
            mapEntryKeyEnd { path, info, entry ->
                //push key ontostack
                currentObjStack.push(info.acc)
                info
            }
            mapEntryValueEnd { path, info, entry ->
                val meKey = currentObjStack.pop()
                val meValue = info.acc
                val mapObj = currentObjStack.peek() as JsonObject
                val mapElements = (mapObj.property[JsonDocument.ENTRIES] ?: JsonArray()) as JsonArray
                val neEl = JsonUnreferencableObject()
                neEl.setProperty(JsonDocument.KEY, meKey)
                neEl.setProperty(JsonDocument.VALUE, meValue)
                mapElements.addElement(neEl)
                //mapObj.withProperty(ELEMENTS, newMap)
                //currentObjStack.push(nObj)
                WalkInfo(info.up, mapObj)
            }
            mapEnd { path, info, map ->
                val obj = currentObjStack.pop()
                WalkInfo(info.up, obj)
            }
            objectBegin { path, info, obj, datatype ->
                val json = JsonReferencableObject(doc, path)
                reference_cache[obj] = json.path
                json.setProperty(JsonDocument.TYPE, JsonDocument.ComplexObjectKind.OBJECT.asJsonString)
                json.setProperty(JsonDocument.CLASS, JsonString(datatype.qualifiedName.value))
                currentObjStack.push(json)
                WalkInfo(path, json)
            }
            objectEnd { path, info, obj, datatype ->
                val obj = currentObjStack.pop()
                WalkInfo(info.up, obj)
            }
            propertyBegin { path, info, property ->
                info
            }
            propertyEnd { path, info, property ->
                val key = path.last()
                val cuObj = currentObjStack.peek() as JsonObject
                cuObj.setProperty(key as String, info.acc)
                //currentObjStack.push(nObj)
                WalkInfo(info.up, cuObj)
            }
        }

        val result = walker.walk(WalkInfo(emptyList(), JsonNull), data)
        doc.root = result.acc
        return doc
    }

    fun <T : Any> toData(jsonString: String, targetKlass: KClass<T>? = null): T {
        //TODO: use a bespoke written JSON parser, it will most likely be faster
        val json = Json.process(jsonString)
        val conv = FromJsonConverter(this.registry)
        return conv.convertTo(emptyList(), json.root, targetKlass) as T
    }

}