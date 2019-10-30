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
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.construct
import net.akehurst.kotlin.komposite.common.set


class FromHJsonConverter(
        val registry: DatatypeRegistry
) {

    private val resolvedReference = mutableMapOf<List<String>, Any>()

    fun convertValue(path: List<String>, json: HJsonValue): Any? {
        return when (json) {
            is HJsonNull -> null
            is HJsonString -> convertPrimitive(json, "String")
            is HJsonNumber -> throw KSerialiserHJsonException("HJsonNumber cannot be converted, not enough type information, please register a primitiveAsObject converter")
            is HJsonBoolean -> convertPrimitive(json, "Boolean")
            is HJsonArray -> convertList(path, json).toTypedArray()
            is HJsonObject -> convertObject(path, json)
            is HJsonReference -> convertReference(path, json)
            else -> throw KSerialiserHJsonException("Cannot convert $json")
        }
    }

    private fun convertPrimitive(json: HJsonValue, typeName: String): Any {
        //val dt = this.registry.findPrimitiveByName(typeName) ?: throw KSerialiserHJsonException("The primitive is not defined in the Komposite configuration")
       // val func = this.primitiveFromHJson[dt] ?: throw KSerialiserHJsonException("Do not know how to convert ${typeName} from json, did you register its converter")
        val mapper = this.registry.findPrimitiveMapperFor(typeName) ?: throw KSerialiserHJsonException("Do not know how to convert ${typeName} from json, did you register its converter")
        return (mapper as PrimitiveMapper<Any, HJsonValue>).toPrimitive(json)
    }

    private fun findByReference(root: HJsonValue, path: List<String>): HJsonValue? {
        return if (path.isEmpty()) {
            root
        } else {
            val head = path.first()
            val tail = path.drop(1)
            val index = head.toIntOrNull()
            val json = when (root) {
                is HJsonArray -> if (null != index) root.elements[index] else throw KSerialiserHJsonException("Path error in reference") //TODO: better error
                is HJsonObject -> {
                    val type = root.property[HJsonDocument.TYPE]
                    when (type) {
                        HJsonDocument.OBJECT -> root.property[head]
                        HJsonDocument.LIST -> {
                            if (null != index) {
                                root.property[HJsonDocument.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserHJsonException("Path error in reference")
                            }
                        }
                        HJsonDocument.SET -> {
                            if (null != index) {
                                root.property[HJsonDocument.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserHJsonException("Path error in reference")
                            }
                        }
                        HJsonDocument.MAP -> {
                            if (null != index) {
                                root.property[HJsonDocument.ELEMENTS]?.asArray()?.elements?.get(index)?.asObject()?.property?.get(HJsonDocument.VALUE)
                            } else {
                                throw KSerialiserHJsonException("Path error in reference")
                            }
                        }
                        else -> throw KSerialiserHJsonException("findByReference doesn't know what to do with a ${type}")
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

    private fun findByReference(root: HJsonValue, path: String): HJsonValue? {
        val p2 = if (path.startsWith("/")) path.substring(1) else path
        val pathList = p2.split("/")
        return this.findByReference(root, pathList)
    }

    private fun convertReference(path: List<String>, json: HJsonReference): Any? {
        return if (resolvedReference.containsKey(json.refPath)) {
            resolvedReference[json.refPath]
        } else {
            val resolved = json.target
            convertValue(json.refPath, resolved)
        }
    }

    private fun convertObject(path: List<String>, json: HJsonObject): Any {
        return if (resolvedReference.containsKey(path)) {
            resolvedReference[path]!!
        } else {
            val type = json.property[HJsonDocument.TYPE]
            when (type) {
                HJsonDocument.ARRAY -> {
                    val elements = json.property[HJsonDocument.ELEMENTS] ?: throw KSerialiserHJsonException("Incorrect JSON, no {KSerialiserHJson.ELEMENTS} property found")
                    convertList(path, elements.asArray()).toTypedArray()
                }
                HJsonDocument.LIST -> {
                    val elements = json.property[HJsonDocument.ELEMENTS] ?: throw KSerialiserHJsonException("Incorrect JSON, no {KSerialiserHJson.ELEMENTS} property found")
                    convertList(path, elements.asArray())
                }
                HJsonDocument.SET -> {
                    val elements = json.property[HJsonDocument.ELEMENTS] ?: throw KSerialiserHJsonException("Incorrect JSON, no {KSerialiserHJson.ELEMENTS} property found")
                    convertList(path, elements.asArray()).toSet()
                }
                HJsonDocument.MAP -> {
                    val elements = json.property[HJsonDocument.ELEMENTS] ?: throw KSerialiserHJsonException("Incorrect JSON, no {KSerialiserHJson.ELEMENTS} property found")
                    convertMap(path, elements.asArray())
                }
                HJsonDocument.OBJECT -> convertObject2Object(path, json)
                HJsonDocument.PRIMITIVE -> convertObject2Primitive(path, json)
                else -> {
                    convertObject2Object(path, json)
                }
            }
        }
    }

    private fun convertObject2Primitive(path: List<String>, json: HJsonObject): Any {
        val clsName = json.property[HJsonDocument.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use qualified name when we can
        //val dt = this.registry.findPrimitiveByName(sn) ?: throw KSerialiserHJsonException("The primitive is not defined in the Komposite configuration")
        //val func = this.primitiveFromHJson[dt] ?: throw KSerialiserHJsonException("Do not know how to convert ${sn} from json, did you register its converter")
        //return func(json)
        val mapper = this.registry.findPrimitiveMapperFor(sn) ?: throw KSerialiserHJsonException("Do not know how to convert ${sn} from json, did you register its converter")
        return (mapper as PrimitiveMapper<Any, HJsonObject>).toPrimitive(json)

    }

    private fun convertObject2Object(path: List<String>, json: HJsonObject): Any {
        val clsName = json.property[HJsonDocument.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use ns
        val dt = registry.findDatatypeByName(sn)
        if (null == dt) {
            throw KSerialiserHJsonException("Cannot find datatype $clsName, is it in the datatype configuration")
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
            dt.explicitNonIdentityProperties.forEach {
                val jsonPropValue = json.property[it.name]
                if (null != jsonPropValue) {
                    val value = this.convertValue(path + it.name, jsonPropValue)
                    it.set(obj, value)
                }
            }
            return obj
        }
    }

    private fun convertList(path: List<String>, json: HJsonArray): List<*> {
        return json.elements.mapIndexed { index, it ->
            this.convertValue(path + "$index", it)
        }
    }

    private fun convertMap(path: List<String>, json: HJsonArray): Map<*, *> {
        return json.elements.mapIndexed { index, jme ->
            val jKey = jme.asObject().property[HJsonDocument.KEY]!!
            val jValue = jme.asObject().property[HJsonDocument.VALUE]!!
            val pathk = path + "key" //TODO: this is not correct
            val pathv = path + "${index}"
            val key = this.convertValue(pathk, jKey)
            val value = this.convertValue(pathv, jValue)
            Pair(key, value)
        }.associate { it }
    }
}