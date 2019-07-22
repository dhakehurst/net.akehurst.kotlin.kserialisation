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

import net.akehurst.kotlin.komposite.api.PrimitiveType
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.construct
import net.akehurst.kotlin.komposite.common.set


class FromJsonConverter(
        val registry: DatatypeRegistry,
        val primitiveFromJson: Map<PrimitiveType, (value: JsonValue) -> Any>,
        val root: JsonValue
) {

    private val resolvedReference = mutableMapOf<String, Any>()

    fun convertValue(path: String, json: JsonValue): Any? {
        return when (json) {
            is JsonNull -> null
            is JsonString -> json.value.replace("\\\"","\"")
            is JsonNumber -> convertNumber(json)
            is JsonBoolean -> json.value
            is JsonArray -> convertList(path, json)
            is JsonObject -> convertObject(path, json)
            is JsonReference -> convertReference(path, json)
            else -> throw KSerialiserJsonException("Cannot convert $json")
        }
    }

    private fun findByReference(root: JsonValue, path: List<String>): JsonValue? {
        return if (path.isEmpty()) {
            root
        } else {
            val head = path.first()
            val tail = path.drop(1)
            val index = head.toIntOrNull()
            val json = when (root) {
                is JsonArray -> if (null != index) root.elements[index] else throw KSerialiserJsonException("Path error in reference") //TODO: better error
                is JsonObject -> {
                    val type = root.property[KSerialiserJson.TYPE]?.asString()?.value
                    when(type) {
                        KSerialiserJson.OBJECT -> root.property[head]
                        KSerialiserJson.LIST -> {
                            if (null != index) {
                                root.property[KSerialiserJson.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserJsonException("Path error in reference")
                            }
                        }
                        KSerialiserJson.SET -> {
                            if (null != index) {
                                root.property[KSerialiserJson.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserJsonException("Path error in reference")
                            }
                        }
                        KSerialiserJson.MAP -> {
                            if (null != index) {
                                root.property[KSerialiserJson.ELEMENTS]?.asArray()?.elements?.get(index)?.asObject()?.property?.get(KSerialiserJson.VALUE)
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

    private fun convertNumber(json: JsonNumber): Number {
        return json.toDouble()
    }

    private fun convertReference(path: String, json: JsonReference): Any? {
        return if (resolvedReference.containsKey(json.path)) {
            resolvedReference[json.path]
        } else {
            val resolved = findByReference(root, json.path) ?: JsonNull
            convertValue(json.path, resolved)
        }
    }

    private fun convertObject(path: String, json: JsonObject): Any {
        return if (resolvedReference.containsKey(path)) {
            resolvedReference[path]!!
        } else {
            val type = json.property[KSerialiserJson.TYPE]?.asString()?.value
            when (type) {
                KSerialiserJson.LIST -> {
                    val elements = json.property[KSerialiserJson.ELEMENTS] ?: throw KSerialiserJsonException("Incorrect JSON, no {KSerialiserJson.ELEMENTS} property found")
                    convertList(path, elements.asArray())
                }
                KSerialiserJson.SET -> {
                    val elements = json.property[KSerialiserJson.ELEMENTS] ?: throw KSerialiserJsonException("Incorrect JSON, no {KSerialiserJson.ELEMENTS} property found")
                    convertSet(path, elements.asArray())
                }
                KSerialiserJson.MAP -> {
                    val elements = json.property[KSerialiserJson.ELEMENTS] ?: throw KSerialiserJsonException("Incorrect JSON, no {KSerialiserJson.ELEMENTS} property found")
                    convertMap(path, elements.asArray())
                }
                KSerialiserJson.OBJECT -> convertObject2Object(path, json)
                KSerialiserJson.PRIMITIVE -> convertObject2Primitive(path, json)
                else -> {
                    convertObject2Object(path, json)
                }
            }
        }
    }

    private fun convertObject2Primitive(path: String, json: JsonObject): Any {
        val clsName = json.property[KSerialiserJson.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use qualified name when we can
        val dt = this.registry.findPrimitiveByName(sn) ?: throw KSerialiserJsonException("The primtive is not defined in the Komposite configuration")
        val func = this.primitiveFromJson[dt] ?: throw KSerialiserJsonException("Do not know how to convert ${sn} from json, did you register its converter")
        return func(json)
    }

    private fun convertObject2Object(path: String, json: JsonObject): Any {
        val clsName = json.property[KSerialiserJson.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use ns
        val dt = registry.findDatatypeByName(sn)
        if (null == dt) {
            throw KSerialiserJsonException("Cannot find datatype $clsName, is it in the datatype configuration")
        } else {
            val idProps = dt.identityProperties.map {
                val jsonPropValue = json.property[it.name]
                if (null == jsonPropValue) {
                    null
                } else {
                    val v = this.convertValue(path + "/${it.name}", jsonPropValue)
                    v
                }
            }
            val obj = dt.construct(*idProps.toTypedArray()) //TODO: need better error when this fails
            // add resolved reference path ASAP, so that we avoid recursion if possible
            resolvedReference[path] = obj

            dt.nonIdentityProperties.forEach {
                if (it.ignore) {
                    //TODO: log it !
                } else {
                    val jsonPropValue = json.property[it.name]
                    if (null != jsonPropValue) {
                        val value = this.convertValue(path + "/${it.name}", jsonPropValue)
                        it.set(obj, value)
                    }
                }
            }
            return obj
        }
    }

    private fun convertList(path: String, json: JsonArray): List<*> {
        return json.elements.mapIndexed { index, it ->
            this.convertValue(path + "/$index", it)
        }
    }

    private fun convertSet(path: String, json: JsonArray): Set<*> {
        return json.elements.mapIndexed { index, it ->
            this.convertValue(path + "/$index", it)
        }.toSet()
    }

    private fun convertMap(path: String, json: JsonArray): Map<*, *> {
        return json.elements.mapIndexed { index, jme ->
            val jKey = jme.asObject().property[KSerialiserJson.KEY]!!
            val jValue = jme.asObject().property[KSerialiserJson.VALUE]!!
            val pathk = path + "/key" //TODO: this is not correct
            val pathv = path + "/${index}"
            val key = this.convertValue(pathk, jKey)
            val value = this.convertValue(pathv, jValue)
            Pair(key, value)
        }.associate { it }
    }
}