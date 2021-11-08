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

package net.akehurst.kotlin.kserialisation.hjson

import net.akehurst.hjson.*
import net.akehurst.kotlin.komposite.api.DatatypeModel
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack
import kotlin.js.JsName
import kotlin.reflect.KClass

class KSerialiserHJsonException : RuntimeException {
    constructor(message: String) : super(message)
}

class KSerialiserHJson() {

    internal val reference_cache = mutableMapOf<Any, List<String>>()

    val registry = DatatypeRegistry()

    class FoundReferenceException : RuntimeException {
        constructor() : super()
    }

    protected fun calcReferencePath(root: Any, targetValue: Any): List<String> {
        return if (reference_cache.containsKey(targetValue)) {
            reference_cache[targetValue]!!
        } else {
            var resultPath:List<String>? = null //TODO: terminate walking early if result found
            val walker = kompositeWalker<List<String>, Boolean>(registry) {
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
            } catch (e:FoundReferenceException) {

            }
            resultPath ?: listOf("${'$'}unknown ${targetValue::class.simpleName}")
        }
    }

    fun confgureFromKompositeString(kompositeModel: String) {
        //TODO: mappers!
        registry.registerFromConfigString(kompositeModel, emptyMap())
    }
    fun confgureFromKompositeModel(kompositeModel: DatatypeModel) {
        //TODO: mappers!
        registry.registerFromKompositeModel(kompositeModel, emptyMap())
    }

    @JsName("registerKotlinStdPrimitives")
    fun registerKotlinStdPrimitives() {
        this.registry.registerFromKompositeModel(DatatypeRegistry.KOTLIN_STD_MODEL, emptyMap())
        this.registerPrimitive(Boolean::class, { value -> HJsonBoolean(value) }, { json -> json.asBoolean().value })
        this.registerPrimitiveAsObject(Byte::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toByte() })
        this.registerPrimitiveAsObject(Short::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toShort() })
        this.registerPrimitiveAsObject(Int::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toInt() })
        this.registerPrimitiveAsObject(Long::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toLong() })
        this.registerPrimitiveAsObject(Float::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toFloat() })
        this.registerPrimitiveAsObject(Double::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toDouble() })
        this.registerPrimitive( String::class, { value ->  HJsonString(value) }, { json -> json.asString().value } )
    }

    @JsName("registerPrimitive")
    fun <T : Any> registerPrimitive(cls: KClass<T>, toHJson: (value: T) -> HJsonValue, toPrimitive: (json: HJsonValue) -> T) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findPrimitiveByClass(cls) ?: throw KSerialiserHJsonException("The primitive is not defined in the Komposite configuration")
        this.registry.registerPrimitiveMapper(PrimitiveMapper.create(cls, HJsonValue::class, toHJson, toPrimitive))
    }

    @JsName("registerPrimitiveAsObject")
    fun <P : Any> registerPrimitiveAsObject(cls: KClass<P>, toHJson: (value: P) -> HJsonValue, fromHJson: (json: HJsonValue) -> P) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findPrimitiveByClass(cls) ?: throw KSerialiserHJsonException("The primitive is not defined in the Komposite configuration")
        val toHJson = { value: P ->
            val obj = HJsonUnreferencableObject()
            obj.setProperty(HJsonDocument.TYPE, HJsonDocument.PRIMITIVE)
            obj.setProperty(HJsonDocument.CLASS, HJsonString(dt.qualifiedName(".")))
            obj.setProperty(HJsonDocument.VALUE,toHJson(value))
            obj
        }
        val toPrimitive = { json:HJsonObject ->
            val jsonValue = json.property[HJsonDocument.VALUE]!!
            fromHJson(jsonValue)
        }
        val mapper = PrimitiveMapper.create(cls, HJsonObject::class, toHJson, toPrimitive)
        this.registry.registerPrimitiveMapper(mapper)
    }

    @JsName("toHJson")
    fun toHJson(root: Any, data: Any): HJsonDocument {
        println("*** root = $root (${root::class}")
        this.reference_cache.clear()
        val doc = HJsonDocument("json")
        var currentObjStack = Stack<HJsonValue>()
        val walker = kompositeWalker<List<String>, HJsonValue>(registry) {
            configure {
                ELEMENTS = HJsonDocument.ELEMENTS
                ENTRIES = HJsonDocument.ENTRIES
                KEY = HJsonDocument.KEY
                VALUE = HJsonDocument.VALUE
            }
            nullValue { path, info ->
                WalkInfo(info.up, HJsonNull)
            }
            primitive { path, info, primitive, mapper ->
                //TODO: use qualified name when/IF JS reflection make it possible!
                val dt = registry.findPrimitiveByName(primitive::class.simpleName!!) ?: throw KSerialiserHJsonException("The primitive is not defined in the Komposite configuration")
                println("*** found primitive ${primitive::class.simpleName!!} = $dt")
                val json = (mapper as PrimitiveMapper<Any, HJsonValue>?)?.toRaw?.invoke(primitive) ?: throw KSerialiserHJsonException("Do not know how to convert ${primitive::class} to json, did you register its converter")
                WalkInfo(info.up, json)
            }
            reference { path, info, value, property ->
                val refPath = calcReferencePath(root, value)
                val ref = HJsonReference(doc,refPath)
                WalkInfo(path, ref)
            }
            collBegin { path, info, type, coll ->
                val elements = HJsonArray()
                currentObjStack.push(elements)
                WalkInfo(info.up, elements)
            }
            collElementEnd { path, info, element ->
                val elements = currentObjStack.peek() as HJsonArray
                elements.addElement(info.acc)
                //val nObj = listObj.withProperty(ELEMENTS, newList)
                //currentObjStack.push(nObj)
                WalkInfo(info.up, elements)
            }
            collEnd { path, info, type, coll ->
                val jsonTypeName = when {
                    type.isArray -> HJsonDocument.ARRAY
                    type.isList -> HJsonDocument.LIST
                    type.isSet -> HJsonDocument.SET
                    else -> throw KSerialiserHJsonException("Unknown type $type")
                }
                val elements = currentObjStack.pop()
                val setObj = HJsonUnreferencableObject()
                setObj.setProperty(HJsonDocument.TYPE, jsonTypeName)
                //ELEMENT_TYPE to HJsonString(type.elementType.qualifiedName), //needed for deserialising empty Arrays
                setObj.setProperty(HJsonDocument.ELEMENTS, elements)
                WalkInfo(info.up, setObj)
            }
            mapBegin { path, info, map ->
                val obj = HJsonUnreferencableObject()
                obj.setProperty(HJsonDocument.TYPE, HJsonDocument.MAP)
                obj.setProperty(HJsonDocument.ENTRIES, HJsonArray())
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
                val mapObj = currentObjStack.peek() as HJsonObject
                val mapElements = (mapObj.property[HJsonDocument.ENTRIES] ?: HJsonArray()) as HJsonArray
                val neEl = HJsonUnreferencableObject()
                neEl.setProperty(HJsonDocument.KEY, meKey)
                neEl.setProperty(HJsonDocument.VALUE, meValue)
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
                val json = HJsonReferencableObject(doc, path)
                reference_cache[obj] = json.path
                json.setProperty(HJsonDocument.TYPE, HJsonDocument.OBJECT)
                json.setProperty(HJsonDocument.CLASS, HJsonString(datatype.qualifiedName(".")))
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
                val cuObj = currentObjStack.peek() as HJsonObject
                cuObj.setProperty(key as String, info.acc)
                //currentObjStack.push(nObj)
                WalkInfo(info.up, cuObj)
            }
        }

        val result = walker.walk(WalkInfo(emptyList(), HJsonNull), data)
        doc.root = result.acc
        return doc
    }

    @JsName("toData")
    fun <T : Any> toData(jsonString: String): T {
        //TODO: use a bespoke written JSON parser, it will most likely be faster
        val json = HJson.process(jsonString)
        val conv = FromHJsonConverter(this.registry)
        return conv.convertValue(emptyList(), json.root) as T
    }


}