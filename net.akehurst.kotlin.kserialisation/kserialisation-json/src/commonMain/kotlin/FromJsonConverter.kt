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
import net.akehurst.kotlin.komposite.common.construct
import net.akehurst.kotlin.komposite.common.set
import net.akehurst.kotlin.komposite.common.valueOf


class FromJsonConverter(
    val registry: DatatypeRegistry
) {

    private val resolvedReference = mutableMapOf<List<String>, Any>()

    fun convertValue(path: List<String>, json: JsonValue): Any? {
        return when (json) {
            is JsonNull -> null
            is JsonString -> convertPrimitive(json, "String")
            is JsonNumber -> throw KSerialiserJsonException("JsonNumber cannot be converted, not enough type information, please register a primitiveAsObject converter")
            is JsonBoolean -> convertPrimitive(json, "Boolean")
            is JsonArray -> convertList(path, json).toTypedArray()
            is JsonObject -> convertObject(path, json)
            is JsonReference -> convertReference(path, json)
            else -> throw KSerialiserJsonException("Cannot convert $json")
        }
    }

    private fun convertPrimitive(json: JsonValue, typeName: String): Any {
        val mapper =
            this.registry.findPrimitiveMapperFor(typeName) ?: throw KSerialiserJsonException("Do not know how to convert ${typeName} from json, did you register its converter")
        return (mapper as PrimitiveMapper<Any, JsonValue>).toPrimitive(json)
    }

    private fun findByReference(json: JsonValue, path: List<String>): JsonValue? {
        return if (path.isEmpty()) {
            json
        } else {
            val head = path.first()
            val tail = path.drop(1)
            val index = head.toIntOrNull()
            val json = when (json) {
                is JsonArray -> if (null != index) json.elements[index] else throw KSerialiserJsonException("Path error in reference") //TODO: better error
                is JsonObject -> {
                    val type = json.property[JsonDocument.TYPE]?.asString()?.value ?: error("Json property '${JsonDocument.TYPE}'Should be a JsonString value")
                    val kind = JsonDocument.ComplexObjectKind.valueOf(type.substringAfter("\$"))
                    when (kind) {
                        JsonDocument.ComplexObjectKind.OBJECT -> json.property[head]
                        JsonDocument.ComplexObjectKind.LIST -> {
                            if (null != index) {
                                json.property[JsonDocument.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserJsonException("Path error in reference")
                            }
                        }
                        JsonDocument.ComplexObjectKind.SET -> {
                            if (null != index) {
                                json.property[JsonDocument.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserJsonException("Path error in reference")
                            }
                        }
                        JsonDocument.ComplexObjectKind.MAP -> {
                            if (null != index) {
                                json.property[JsonDocument.ENTRIES]?.asArray()?.elements?.get(index)?.asObject()?.property?.get(JsonDocument.VALUE)
                            } else {
                                throw KSerialiserJsonException("Path error in reference")
                            }
                        }
                        else -> throw KSerialiserJsonException("findByReference doesn't know what to do with a ${type}")
                    }
                }
                else -> null
            }
            if (null == json) {
                null
            } else {
                findByReference(json, tail)
            }
        }
    }

    private fun findByReference(root: JsonValue, path: String): JsonValue? {
        val p2 = if (path.startsWith("/")) path.substring(1) else path
        val pathList = p2.split("/")
        return this.findByReference(root, pathList)
    }

    private fun convertReference(path: List<String>, json: JsonReference): Any? {
        return if (resolvedReference.containsKey(json.refPath)) {
            resolvedReference[json.refPath]
        } else {
            val resolved = json.target
            convertValue(json.refPath, resolved)
        }
    }

    private fun convertObject(path: List<String>, json: JsonObject): Any {
        return if (resolvedReference.containsKey(path)) {
            resolvedReference[path]!!
        } else {
            val type = json.property[JsonDocument.TYPE]?.asString()?.value ?: error("Json property '${JsonDocument.TYPE}'Should be a JsonString value")
            val kind = JsonDocument.ComplexObjectKind.valueOf(type.substringAfter("\$"))
            when (kind) {
                JsonDocument.ComplexObjectKind.ARRAY -> {
                    val elements = json.property[JsonDocument.ELEMENTS] ?: throw KSerialiserJsonException("Incorrect JSON, no ${JsonDocument.ELEMENTS} property found")
                    convertList(path, elements.asArray()).toTypedArray()
                }
                JsonDocument.ComplexObjectKind.LIST -> {
                    val elements = json.property[JsonDocument.ELEMENTS] ?: throw KSerialiserJsonException("Incorrect JSON, no ${JsonDocument.ELEMENTS} property found")
                    convertList(path, elements.asArray())
                }
                JsonDocument.ComplexObjectKind.SET -> {
                    val elements = json.property[JsonDocument.ELEMENTS] ?: throw KSerialiserJsonException("Incorrect JSON, no ${JsonDocument.ELEMENTS} property found")
                    convertList(path, elements.asArray()).toSet()
                }
                JsonDocument.ComplexObjectKind.MAP -> {
                    val elements = json.property[JsonDocument.ENTRIES] ?: throw KSerialiserJsonException("Incorrect JSON, no ${JsonDocument.ENTRIES} property found")
                    convertMap(path, elements.asArray())
                }
                JsonDocument.ComplexObjectKind.OBJECT -> convertObject2Object(path, json)
                JsonDocument.ComplexObjectKind.PRIMITIVE -> convertObject2Primitive(path, json)
                JsonDocument.ComplexObjectKind.ENUM -> convertObject2Enum(path, json) ?: throw KSerialiserJsonException("Incorrect JSON, no enum value found for ${json.toFormattedJsonString("","")}")
                else -> {
                    convertObject2Object(path, json)
                }
            }
        }
    }

    private fun convertObject2Primitive(path: List<String>, json: JsonObject): Any {
        val clsName = json.property[JsonDocument.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use qualified name when we can
        val mapper = this.registry.findPrimitiveMapperFor(sn) ?: throw KSerialiserJsonException("Do not know how to convert ${sn} from json, did you register its converter")
        return (mapper as PrimitiveMapper<Any, JsonObject>).toPrimitive(json)

    }

    private fun convertObject2Enum(path: List<String>, json: JsonObject): Enum<*>? {
        val clsName = json.property[JsonDocument.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use qualified name when we can
        val et = registry.findEnumByName(sn) ?: error("Cannot find enum $clsName, is it in the konfiguration")
        val value = json.property[JsonDocument.VALUE]!!.asString().value
        return et.valueOf(value)
    }


    private fun convertObject2Object(path: List<String>, json: JsonObject): Any {
        val clsName = json.property[JsonDocument.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use ns
        val dt = registry.findDatatypeByName(sn)
        if (null == dt) {
            throw KSerialiserJsonException("Cannot find datatype $clsName, is it in the konfiguration")
        } else {
            val idProps = dt.identityProperties.map {
                val jsonPropValue = json.property[it.name]
                if (null == jsonPropValue) {
                    null
                } else {
                    val v = this.convertValue(path + it.name, jsonPropValue)
                    v
                }
            }

            val obj = dt.construct(*idProps.toTypedArray()) //TODO: need better error when this fails
            // add resolved reference path ASAP, so that we avoid recursion if possible
            resolvedReference[path] = obj

            // TODO: change this to enable nonExplicit properties, once JS reflection works
            dt.allExplicitNonIdentityProperties.forEach {
                val jsonPropValue = json.property[it.name]
                if (null != jsonPropValue) {
                    val value = this.convertValue(path + it.name, jsonPropValue)
                    it.set(obj, value)
                }
            }
            return obj
        }
    }

    private fun convertList(path: List<String>, json: JsonArray): List<*> {
        val path_elements = path + JsonDocument.ELEMENTS
        return json.elements.mapIndexed { index, it ->
            this.convertValue(path_elements + "$index", it)
        }
    }

    private fun convertMap(path: List<String>, json: JsonArray): Map<*, *> {
        val path_elements = path + JsonDocument.ENTRIES
        return json.elements.mapIndexed { index, jme ->
            val path_entry = path_elements + "${index}"
            val jKey = jme.asObject().property[JsonDocument.KEY]!!
            val jValue = jme.asObject().property[JsonDocument.VALUE]!!
            val pathk = path_entry + JsonDocument.KEY
            val pathv = path_entry + JsonDocument.VALUE
            val key = this.convertValue(pathk, jKey)
            val value = this.convertValue(pathv, jValue)
            Pair(key, value)
        }.associate { it }
    }
}