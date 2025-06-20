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
import net.akehurst.kotlinx.komposite.common.*
import net.akehurst.language.agl.expressions.processor.constructDataType
import net.akehurst.language.agl.expressions.processor.constructValueType
import net.akehurst.language.agl.expressions.processor.objectInstance
import net.akehurst.language.agl.expressions.processor.set
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.StdLibDefault
import kotlin.reflect.KClass


class FromHJsonConverter(
    val registry: DatatypeRegistry
) {

    private val resolvedReference = mutableMapOf<List<String>, Any>()

    fun <T : Any> convertTo(path: List<String>, hjson: HJsonValue, targetKlass: KClass<T>? = null): T? {
        val targetType = targetKlass?.let { registry.findTypeDeclarationByKClass(it) }
        val type = targetType?.type()
        return convertValue(path, hjson, type) as T?
    }

    private fun convertValue(path: List<String>, hjson: HJsonValue, targetType: TypeInstance?): Any? {
        return when (hjson) {
            is HJsonNull -> null
            is HJsonString -> convertPrimitive(hjson, targetType?.typeName?.value ?: "String")
            is HJsonNumber -> convertNumber(hjson, targetType)
            is HJsonBoolean -> convertPrimitive(hjson, targetType?.typeName?.value ?: "Boolean")
            is HJsonArray -> convertList(path, hjson, targetType).toTypedArray()
            is HJsonObject -> convertObject(path, hjson, targetType)
            is HJsonReference -> convertReference(path, hjson)
            else -> throw KSerialiserHJsonException("Cannot convert $hjson")
        }
    }

    private fun convertNumber(hjson: HJsonNumber, targetType: TypeInstance?): Any {
        return when {
            null != targetType && targetType.resolvedDeclaration is PrimitiveType -> when (targetType.typeName.value) {
                "Int" -> hjson.toInt()
                "Double" -> hjson.toDouble()
                "Long" -> hjson.toLong()
                "Float" -> hjson.toFloat()
                "Byte" -> hjson.toByte()
                "Short" -> hjson.toShort()
                else -> error("HJsonNumber cannot be converted, not a std type, please use a primitiveAsObject converter")
            }

            else -> hjson.toDouble()
            //else -> error("HJsonNumber cannot be converted, not enough type information, please register a primitiveAsObject converter")
        }
    }

    private fun convertPrimitive(json: HJsonValue, typeName: String): Any {
        //val dt = this.registry.findPrimitiveByName(typeName) ?: throw KSerialiserHJsonException("The primitive is not defined in the Komposite configuration")
        // val func = this.primitiveFromHJson[dt] ?: throw KSerialiserHJsonException("Do not know how to convert ${typeName} from json, did you register its converter")
        val mapper = this.registry.findPrimitiveMapperBySimpleName(typeName)
            ?: throw KSerialiserHJsonException("Do not know how to convert ${typeName} from json, did you register its converter")
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
                    val type = root.property[HJsonDocument.KIND]?.asString()?.value ?: error("Json property '${HJsonDocument.KIND}'Should be a JsonString value")
                    val kind = HJsonDocument.ComplexObjectKind.valueOf(type.substringAfter("\$"))
                    when (kind) {
                        HJsonDocument.ComplexObjectKind.OBJECT -> root.property[head]
                        HJsonDocument.ComplexObjectKind.LIST -> {
                            if (null != index) {
                                root.property[HJsonDocument.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserHJsonException("Path error in reference")
                            }
                        }

                        HJsonDocument.ComplexObjectKind.SET -> {
                            if (null != index) {
                                root.property[HJsonDocument.ELEMENTS]?.asArray()?.elements?.get(index)
                            } else {
                                throw KSerialiserHJsonException("Path error in reference")
                            }
                        }

                        HJsonDocument.ComplexObjectKind.MAP -> {
                            if (null != index) {
                                root.property[HJsonDocument.ENTRIES]?.asArray()?.elements?.get(index)
                                    ?.asObject()?.property?.get(HJsonDocument.VALUE)
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
            convertValue(json.refPath, resolved, null)
        }
    }

    private fun convertObject(path: List<String>, json: HJsonObject, targetType: TypeInstance?): Any? {
        return if (resolvedReference.containsKey(path)) {
            resolvedReference[path]!!
        } else {
            val type = json.property[HJsonDocument.KIND]?.asString()?.value ?: error("Json property '${HJsonDocument.KIND}'Should be a JsonString value")
            val kind = HJsonDocument.ComplexObjectKind.valueOf(type.substringAfter("\$"))
            when (kind) {
                HJsonDocument.ComplexObjectKind.ARRAY -> {
                    val elements = json.property[HJsonDocument.ELEMENTS]
                        ?: throw KSerialiserHJsonException("Incorrect JSON, no ${HJsonDocument.ELEMENTS} property found")
                    convertList(path, elements.asArray(), targetType).toTypedArray()
                }

                HJsonDocument.ComplexObjectKind.LIST -> {
                    val elements = json.property[HJsonDocument.ELEMENTS]
                        ?: throw KSerialiserHJsonException("Incorrect JSON, no ${HJsonDocument.ELEMENTS} property found")
                    convertList(path, elements.asArray(), targetType)
                }

                HJsonDocument.ComplexObjectKind.SET -> {
                    val elements = json.property[HJsonDocument.ELEMENTS]
                        ?: throw KSerialiserHJsonException("Incorrect JSON, no ${HJsonDocument.ELEMENTS} property found")
                    convertList(path, elements.asArray(), targetType).toSet()
                }

                HJsonDocument.ComplexObjectKind.MAP -> {
                    val elements = json.property[HJsonDocument.ENTRIES]
                        ?: throw KSerialiserHJsonException("Incorrect JSON, no ${HJsonDocument.ENTRIES} property found")
                    convertMap(path, elements.asArray(), targetType)
                }

                HJsonDocument.ComplexObjectKind.SINGLETON -> convertObject2Singleton(path, json, targetType)
                HJsonDocument.ComplexObjectKind.OBJECT -> convertObject2Object(path, json, targetType)
                HJsonDocument.ComplexObjectKind.PRIMITIVE -> convertObject2Primitive(path, json, targetType)
                //TODO: HJsonDocument.ENUM -> convertObject2Enum()
                else -> {
                    convertObject2Object(path, json, targetType)
                }
            }
        }
    }

    private fun convertObject2Singleton(path: List<String>, json: HJsonObject, targetType: TypeInstance?): Any {
        val clsName = json.property[HJsonDocument.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = SimpleName(clsName.substringAfterLast("."))
        //TODO: use qualified name when we can
        val st = registry.findFirstDefinitionByNameOrNull(sn) as SingletonType? ?: error("Cannot find SingletonType $clsName, is it in the konfiguration")
        //val value = json.property[JsonDocument.VALUE]!!.asString().value
        val obj = st.objectInstance()
        resolvedReference[path] = obj
        return obj
    }

    private fun convertObject2Primitive(path: List<String>, json: HJsonObject, targetType: TypeInstance?): Any {
        val clsName = json.property[HJsonDocument.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use qualified name when we can
        //val dt = this.registry.findPrimitiveByName(sn) ?: throw KSerialiserHJsonException("The primitive is not defined in the Komposite configuration")
        //val func = this.primitiveFromHJson[dt] ?: throw KSerialiserHJsonException("Do not know how to convert ${sn} from json, did you register its converter")
        //return func(json)
        val mapper = this.registry.findPrimitiveMapperBySimpleName(sn)
            ?: throw KSerialiserHJsonException("Do not know how to convert ${sn} from json, did you register its converter")
        return (mapper as PrimitiveMapper<Any, HJsonObject>).toPrimitive(json)

    }

    private fun convertObject2Object(path: List<String>, json: HJsonObject, targetType: TypeInstance?): Any? {
        val clsName = json.property[HJsonDocument.CLASS]?.asString()?.value?.let { QualifiedName(it) }
            ?: targetType?.qualifiedTypeName
            ?: error("Cannot determine target type for HJson object")
        val ns = clsName.front.value
        val sn = clsName.last
        //TODO: use ns and Qualified name when JS supports it
        val dt = registry.findFirstDefinitionByNameOrNull(sn)
        val obj = when (dt) {
            null -> error("Cannot find datatype $clsName, is it in the konfiguration")
            is ValueType -> {
                val jsonPropValue = json.property[HJsonDocument.VALUE]!!
                val propClsName = dt.valueProperty.typeInstance.typeName.value
                val value = this.convertPrimitive(jsonPropValue, propClsName)
                val obj = dt.constructValueType(value) //TODO: need better error when this fails
                obj
            }

            is DataType -> {
                val constructorParams = dt.constructors[0].parameters
                val consArgs = constructorParams.map {
                    val jsonPropValue = json.property[it.name.value]
                    if (null == jsonPropValue) {
                        null
                    } else {
                        val propType = it.typeInstance
                        val v = this.convertValue(path + it.name.value, jsonPropValue, propType)
                        v
                    }
                }
                when {
                    StdLibDefault.Pair.qualifiedName == clsName -> {
                        val first = consArgs[0]
                        val second = consArgs[1]
                        Pair(first, second)
                    }

                    else -> {
                        val obj = dt.constructDataType(*consArgs.toTypedArray()) //TODO: need better error when this fails
                        // add resolved reference path ASAP, so that we avoid recursion if possible
                        resolvedReference[path] = obj

                        // TODO: change this to enable nonExplicit properties, once JS reflection works
                        val memberProps = dt.allProperty.values
                            .filter { it.isReadWrite }
                            .filterNot { mp -> constructorParams.any { cp -> cp.name.value == mp.name.value } }
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
                }
            }

            else -> error("Internal error: Cannot construct ''$clsName', '${dt::class.simpleName}' is not handled")
        }
        return obj
    }

    private fun convertList(path: List<String>, json: HJsonArray, targetType: TypeInstance?): List<*> {
        val elementType = targetType?.typeArguments?.firstOrNull()
        return json.elements.mapIndexed { index, it ->
            this.convertValue(path + "$index", it, elementType?.type)
        }
    }

    private fun convertMap(path: List<String>, json: HJsonArray, targetType: TypeInstance?): Map<*, *> {
        return json.elements.mapIndexed { index, jme ->
            val jKey = jme.asObject().property[HJsonDocument.KEY]!!
            val jValue = jme.asObject().property[HJsonDocument.VALUE]!!
            val pathk = path + "key" //TODO: this is not correct
            val pathv = path + "${index}"
            val key = this.convertValue(pathk, jKey, null)
            val value = this.convertValue(pathv, jValue, null)
            Pair(key, value)
        }.associate { it }
    }
}