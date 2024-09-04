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
import net.akehurst.kotlin.komposite.common.*
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.typemodel.api.*
import kotlin.reflect.KClass


class FromJsonConverter(
    val registry: DatatypeRegistry
) {

    private val resolvedReference = mutableMapOf<List<String>, Any>()

    fun <T : Any> convertTo(path: List<String>, json: JsonValue, targetKlass: KClass<T>? = null): T? {
        val targetType = targetKlass?.let { registry.findTypeDeclarationByKClass(it) }
        val type = targetType?.type()
        return convertValue(path, json, type) as T?
    }

   private fun convertValue(path: List<String>, json: JsonValue, targetType: TypeInstance?): Any? {
        return when (json) {
            is JsonNull -> null
            is JsonString -> convertPrimitive(json, "String")
            is JsonNumber -> convertNumber(json, targetType)
            is JsonBoolean -> convertPrimitive(json, "Boolean")
            is JsonArray -> convertList(path, json, targetType).toTypedArray()
            is JsonObject -> convertObject(path, json, targetType)
            is JsonReference -> convertReference(path, json)
            else -> error("Cannot convert $json")
        }
    }

    private fun convertNumber(json: JsonNumber, targetType: TypeInstance?): Any {
        return when {
            null != targetType && targetType.declaration is PrimitiveType -> when (targetType.typeName.value) {
                "Int" -> json.toInt()
                "Double" -> json.toDouble()
                "Long" -> json.toLong()
                "Float" -> json.toFloat()
                "Byte" -> json.toByte()
                "Short" -> json.toShort()
                else -> error("HJsonNumber cannot be converted, not a std type, please use a primitiveAsObject converter")
            }

            else -> json.toDouble()
            //else -> error("HJsonNumber cannot be converted, not enough type information, please register a primitiveAsObject converter")
        }
    }

    private fun convertPrimitive(json: JsonValue, typeName: String): Any {
        val mapper = this.registry.findPrimitiveMapperBySimpleName(typeName)
                ?: throw KSerialiserJsonException("Do not know how to convert ${typeName} from json, did you register its converter")
        return (mapper as PrimitiveMapper<Any, JsonValue>).toPrimitive(json)
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
                    val type = root.property[JsonDocument.TYPE]?.asString()?.value ?: error("Json property '${JsonDocument.TYPE}'Should be a JsonString value")
                    val kind = JsonDocument.ComplexObjectKind.valueOf(type.substringAfter("\$"))
                    when (kind) {
                        JsonDocument.ComplexObjectKind.OBJECT -> root.property[head]
                        JsonDocument.ComplexObjectKind.LIST -> {
                            if (null != index) {
                                root.property[JsonDocument.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserJsonException("Path error in reference")
                            }
                        }

                        JsonDocument.ComplexObjectKind.SET -> {
                            if (null != index) {
                                root.property[JsonDocument.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserJsonException("Path error in reference")
                            }
                        }

                        JsonDocument.ComplexObjectKind.MAP -> {
                            if (null != index) {
                                root.property[JsonDocument.ENTRIES]?.asArray()?.elements?.get(index)?.asObject()?.property?.get(JsonDocument.VALUE)
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
            convertValue(json.refPath, resolved, null)
        }
    }

    private fun convertObject(path: List<String>, json: JsonObject, targetType: TypeInstance?): Any {
        return if (resolvedReference.containsKey(path)) {
            resolvedReference[path]!!
        } else {
            val type = json.property[JsonDocument.TYPE]?.asString()?.value ?: error("Json property '${JsonDocument.TYPE}'Should be a JsonString value")
            val kind = JsonDocument.ComplexObjectKind.valueOf(type.substringAfter("\$"))
            when (kind) {
                JsonDocument.ComplexObjectKind.ARRAY -> {
                    val elements = json.property[JsonDocument.ELEMENTS] ?: throw KSerialiserJsonException("Incorrect JSON, no ${JsonDocument.ELEMENTS} property found")
                    convertList(path, elements.asArray(), targetType).toTypedArray()
                }

                JsonDocument.ComplexObjectKind.LIST -> {
                    val elements = json.property[JsonDocument.ELEMENTS] ?: throw KSerialiserJsonException("Incorrect JSON, no ${JsonDocument.ELEMENTS} property found")
                    convertList(path, elements.asArray(), targetType)
                }

                JsonDocument.ComplexObjectKind.SET -> {
                    val elements = json.property[JsonDocument.ELEMENTS] ?: throw KSerialiserJsonException("Incorrect JSON, no ${JsonDocument.ELEMENTS} property found")
                    convertList(path, elements.asArray(), targetType).toSet()
                }

                JsonDocument.ComplexObjectKind.MAP -> {
                    val elements = json.property[JsonDocument.ENTRIES] ?: throw KSerialiserJsonException("Incorrect JSON, no ${JsonDocument.ENTRIES} property found")
                    convertMap(path, elements.asArray(), targetType)
                }

                JsonDocument.ComplexObjectKind.OBJECT -> convertObject2Object(path, json, targetType)
                JsonDocument.ComplexObjectKind.PRIMITIVE -> convertObject2Primitive(path, json, targetType)
                JsonDocument.ComplexObjectKind.ENUM -> convertObject2Enum(path, json) ?: throw KSerialiserJsonException("Incorrect JSON, no enum value found for ${json.toFormattedJsonString("", "")}")
                else -> {
                    convertObject2Object(path, json, targetType)
                }
            }
        }
    }

    private fun convertObject2Primitive(path: List<String>, json: JsonObject, targetType: TypeInstance?): Any {
        val clsName = json.property[JsonDocument.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use qualified name when we can
        val mapper = this.registry.findPrimitiveMapperBySimpleName(sn) ?: throw KSerialiserJsonException("Do not know how to convert ${sn} from json, did you register its converter")
        return (mapper as PrimitiveMapper<Any, JsonObject>).toPrimitive(json)

    }

    private fun convertObject2Enum(path: List<String>, json: JsonObject): Enum<*>? {
        val clsName = json.property[JsonDocument.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = SimpleName(clsName.substringAfterLast("."))
        //TODO: use qualified name when we can
        val et = registry.findFirstByNameOrNull(sn) as EnumType? ?: error("Cannot find enum $clsName, is it in the konfiguration")
        val value = json.property[JsonDocument.VALUE]!!.asString().value
        return et.valueOf(value)
    }

    private fun convertObject2Object(path: List<String>, json: JsonObject, targetType: TypeInstance?): Any {
        val clsName = json.property[JsonDocument.CLASS]?.asString()?.value?.let { QualifiedName(it) }
            ?: targetType?.qualifiedTypeName
            ?: error("Cannot determine target type for Json object")
        val ns = clsName.front
        val sn = clsName.last
        //TODO: use ns
        val dt = registry.findFirstByNameOrNull(sn)
        val obj = when(dt) {
            null -> error("Cannot find datatype $clsName, is it in the registered Konfigurations")
            is SingletonType -> dt.objectInstance()
            is DataType -> {
                val constructorProps = dt.property.filter {
                    it.characteristics.any { it==PropertyCharacteristic.IDENTITY || it==PropertyCharacteristic.CONSTRUCTOR }
                }.sortedBy { it.index }
                val consArgs = constructorProps.map {
                    val jsonPropValue = json.property[it.name.value]
                    if (null == jsonPropValue) {
                        null
                    } else {
                        val propType = it.typeInstance
                        val v = this.convertValue(path + it.name.value, jsonPropValue, propType)
                        v
                    }
                }

                val obj = dt.construct(*consArgs.toTypedArray()) //TODO: need better error when this fails
                // add resolved reference path ASAP, so that we avoid recursion if possible
                resolvedReference[path] = obj

                // TODO: change this to enable nonExplicit properties, once JS reflection works
                val memberProps = dt.allProperty.values.filter {
                    it.characteristics.any { it==PropertyCharacteristic.MEMBER }
                }
                memberProps.forEach {
                    val jsonPropValue = json.property[it.name.value]
                    if (null != jsonPropValue) {
                        val propType = it.typeInstance
                        val value = this.convertValue(path + it.name.value, jsonPropValue, propType)
                        it.set(obj, value)
                    }
                }
                obj
            }
            else -> error("Internal error: Cannot construct ''$clsName', '${dt::class.simpleName}' is not handled")
        }
        return obj
    }

    private fun convertList(path: List<String>, json: JsonArray, targetType: TypeInstance?): List<*> {
        val elementType = targetType?.typeArguments?.firstOrNull()
        val path_elements = path + JsonDocument.ELEMENTS
        return json.elements.mapIndexed { index, it ->
            this.convertValue(path_elements + "$index", it, elementType)
        }
    }

    private fun convertMap(path: List<String>, json: JsonArray, targetType: TypeInstance?): Map<*, *> {
        val path_elements = path + JsonDocument.ENTRIES
        return json.elements.mapIndexed { index, jme ->
            val path_entry = path_elements + "${index}"
            val jKey = jme.asObject().property[JsonDocument.KEY]!!
            val jValue = jme.asObject().property[JsonDocument.VALUE]!!
            val pathk = path_entry + JsonDocument.KEY
            val pathv = path_entry + JsonDocument.VALUE
            val key = this.convertValue(pathk, jKey, null)
            val value = this.convertValue(pathv, jValue, null)
            Pair(key, value)
        }.associate { it }
    }
}