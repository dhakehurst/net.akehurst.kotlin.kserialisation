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

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.PrimitiveType
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.KompositeWalker
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack
import net.akehurst.kotlinx.reflect.ModuleRegistry
import kotlin.js.JsName
import kotlin.reflect.KClass

class KSerialiserJsonException : RuntimeException {
    constructor(message: String) : super(message)
}

class KSerialiserJson() {

    companion object {
        val TYPE = "${'$'}type"     // PRIMITIVE | OBJECT | LIST | SET | MAP
        val OBJECT = "${'$'}OBJECT"
        val CLASS = "${'$'}class"
        val PRIMITIVE = "${'$'}PRIMITIVE"
        val KEY = "${'$'}key"
        val VALUE = "${'$'}value"
        val LIST = "${'$'}LIST"
        val MAP = "${'$'}MAP"
        val SET = "${'$'}SET"
        val ELEMENTS = "${'$'}elements"
    }

    private val reference_cache = mutableMapOf<Any, String>()
    private val primitiveToJson = mutableMapOf<PrimitiveType, (value: Any) -> JsonValue>()
    private val primitiveFomJson = mutableMapOf<PrimitiveType, (value: JsonValue) -> Any>()

    val registry = DatatypeRegistry()
    /*
    val jsonReg = DatatypeRegistry()

    init {
        jsonReg.registerFromConfigString("""
            namespace net.akehurst.kotlin.kserialisation.json {
                primitive JsonString
                primitive JsonNumber
                primitive JsonBoolean
                primitive JsonNull
                
                collection JsonArray
                
                datatype JsonObject {
                }
                
                datatype JsonArray {
                }
            }
        """)
    }
*/
    protected fun calcReferencePath(root: Any, targetValue: Any): String {
        return if (reference_cache.containsKey(targetValue)) {
            reference_cache[targetValue]!!
        } else {
            var resultPath: String? = null //TODO: terminate walking early if result found
            val walker = kompositeWalker<List<String>, Boolean>(registry) {
                collBegin { key, info, coll ->
                    val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                    WalkInfo(path, info.acc)
                }
                mapBegin { key, info, map ->
                    val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                    WalkInfo(path, info.acc)
                }
                //               mapEntryValueBegin { key, info, entry ->
                //                   val path = if (key== KompositeWalker.ROOT) info.path else info.path + key.toString()
                //                   WalkInfo(path, info.acc)
                //               }
                objectBegin { key, info, obj, datatype ->
                    val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                    if (obj == targetValue) {
                        resultPath = path.joinToString("/")
                    }
                    WalkInfo(path, obj == targetValue)
                }
                propertyBegin { key, info, property ->
                    val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                    WalkInfo(path, info.acc)
                }
            }

            val result = walker.walk(WalkInfo(emptyList(), false), root)
            resultPath ?: "${'$'}unknown ${targetValue::class.simpleName}"
        }
    }

    fun confgureDatatypeModel(config: String) {
        registry.registerFromConfigString(config)
    }

    @JsName("registerModule")
    fun registerModule(moduleName: String) {
        ModuleRegistry.register(moduleName)
    }

    @JsName("registerKotlinStdPrimitives")
    fun registerKotlinStdPrimitives() {
        this.confgureDatatypeModel(DatatypeRegistry.KOTLIN_STD)
        this.registerPrimitive(Boolean::class, { value -> JsonBoolean(value) }, { json -> json.asBoolean().value })
        this.registerPrimitiveAsObject(Byte::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toByte() })
        this.registerPrimitiveAsObject(Short::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toShort() })
        this.registerPrimitiveAsObject(Int::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toInt() })
        this.registerPrimitiveAsObject(Long::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toLong() })
        this.registerPrimitiveAsObject(Float::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toFloat() })
        this.registerPrimitiveAsObject(Double::class, { value -> JsonNumber(value.toString()) }, { json -> json.asNumber().toDouble() })
        this.registerPrimitive(
                String::class,
                { value ->
                    JsonString(value //
                            .replace("\\", "\\\\")
                            .replace("\b", "\\b")
                            .replace("\u000C", "\\f")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t")
                            .replace("\"", "\\\"")

                    )
                },
                { json ->
                    json.asString().value //
                            .replace("\\b", "\b")
                            .replace("\\f", "\u000C")
                            .replace("\\n", "\n")
                            .replace("\\r", "\r")
                            .replace("\\t", "\t")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                }
        )
    }

    @JsName("registerPrimitive")
    fun <T : Any> registerPrimitive(cls: KClass<T>, toJson: (value: T) -> JsonValue, fromJson: (json: JsonValue) -> T) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findPrimitiveByClass(cls) ?: throw KSerialiserJsonException("The primitive is not defined in the Komposite configuration")
        primitiveToJson[dt] = toJson as (Any) -> JsonValue
        primitiveFomJson[dt] = fromJson
    }

    @JsName("registerPrimitiveAsObject")
    fun <T : Any> registerPrimitiveAsObject(cls: KClass<T>, toJson: (value: T) -> JsonValue, fromJson: (json: JsonValue) -> T) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findPrimitiveByClass(cls) ?: throw KSerialiserJsonException("The primitive is not defined in the Komposite configuration")
        primitiveToJson[dt] = { value: T ->
            JsonObject(mapOf(
                    TYPE to JsonString(KSerialiserJson.PRIMITIVE),
                    CLASS to JsonString(dt.qualifiedName(".")),
                    VALUE to toJson(value)
            ))
        } as (Any) -> JsonValue
        primitiveFomJson[dt] = { json ->
            val jsonValue = json.asObject().property[KSerialiserJson.VALUE]!!
            fromJson(jsonValue)
        }
    }

    @JsName("toJson")
    fun toJson(root: Any, data: Any): String {
        var currentObjStack = Stack<JsonValue>()
        val walker = kompositeWalker<List<String>, JsonValue>(registry) {
            nullValue { key, info ->
                WalkInfo(info.path + key.toString(), JsonNull)
            }
            primitive { key, info, value ->
                //TODO: use qualified name when we can!
                val dt = registry.findPrimitiveByName(value::class.simpleName!!) ?: throw KSerialiserJsonException("The primtive is not defined in the Komposite configuration")
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val func = primitiveToJson[dt] ?: throw KSerialiserJsonException("Do not know how to convert ${value::class} to json, did you register its converter")
                val json = func(value)
                WalkInfo(path, json)
            }
            reference { key, info, value, property ->
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val refPath = calcReferencePath(root, value)
                val ref = JsonReference(refPath)
                WalkInfo(path, ref)
            }
            collBegin { key, info, coll ->
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val collTypeName = when (coll) {
                    is List<*> -> LIST
                    is Set<*> -> SET
                    else -> throw KSerialiserJsonException("Unknown collection type ${coll::class.simpleName}")
                }
                val listObj = JsonObject(mapOf(
                        TYPE to JsonString(collTypeName),
                        ELEMENTS to JsonArray()
                ))
                currentObjStack.push(listObj)
                WalkInfo(info.path, listObj)
            }
            collElementEnd { key, info, element ->
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val listObj = currentObjStack.peek() as JsonObject
                val list = (listObj.property[ELEMENTS] ?: JsonArray()) as JsonArray
                list.addElement(info.acc)
                //val nObj = listObj.withProperty(ELEMENTS, newList)
                //currentObjStack.push(nObj)
                WalkInfo(path, listObj)
            }
            collEnd { key, info, coll ->
                val listObj = currentObjStack.pop()
                WalkInfo(info.path, listObj)
            }
            mapBegin { key, info, map ->
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val listObj = JsonObject(mapOf(
                        TYPE to JsonString(MAP),
                        ELEMENTS to JsonArray()
                ))
                currentObjStack.push(listObj)
                WalkInfo(path, listObj)
            }
            mapEntryKeyEnd { key, info, entry ->
                //push key ontostack
                currentObjStack.push(info.acc)
                info
            }
            mapEntryValueEnd { key, info, entry ->
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val meKey = currentObjStack.pop()
                val meValue = info.acc
                val mapObj = currentObjStack.peek() as JsonObject
                val mapElements = (mapObj.property[ELEMENTS] ?: JsonArray()) as JsonArray
                val neEl = JsonObject(mapOf(
                        KEY to meKey,
                        VALUE to meValue
                ))
                mapElements.addElement(neEl)
                //mapObj.withProperty(ELEMENTS, newMap)
                //currentObjStack.push(nObj)
                WalkInfo(path, mapObj)
            }
            mapEnd { key, info, map ->
                val obj = currentObjStack.pop()
                WalkInfo(info.path, obj)
            }
            objectBegin { key, info, obj, datatype ->
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val obj = JsonObject(mutableMapOf(
                        TYPE to JsonString(OBJECT),
                        CLASS to JsonString(datatype.qualifiedName("."))
                ))
                currentObjStack.push(obj)
                WalkInfo(path, obj)
            }
            objectEnd { key, info, obj, datatype ->
                val obj = currentObjStack.pop()
                WalkInfo(info.path, obj)
            }
            propertyBegin { key, info, property ->
                info
            }
            propertyEnd { key, info, property ->
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val cuObj = currentObjStack.peek() as JsonObject
                cuObj.setProperty(key as String, info.acc)
                //currentObjStack.push(nObj)
                WalkInfo(path, cuObj)
            }
        }

        val result = walker.walk(WalkInfo(emptyList(), JsonNull), data)
        return result.acc.toJsonString()
    }

    @JsName("toData")
    fun toData(jsonString: String): Any? {
        //TODO: use a bespoke written JSON parser, it will most likely be faster
        val json = Json.process(jsonString)
        val conv = FromJsonConverter(this.registry, this.primitiveFomJson, json)
        return conv.convertValue("", json)
    }


}