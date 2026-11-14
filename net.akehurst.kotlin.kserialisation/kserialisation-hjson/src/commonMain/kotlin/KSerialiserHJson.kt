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
import net.akehurst.kotlinx.collections.mutableStackOf
import net.akehurst.kotlinx.komposite.common.DatatypeRegistry
import net.akehurst.kotlinx.komposite.common.PrimitiveMapper
import net.akehurst.kotlinx.komposite.common.WalkInfo
import net.akehurst.kotlinx.komposite.common.kompositeWalker
import net.akehurst.language.types.api.TypesDomain
import kotlin.reflect.KClass

/** Thrown when a value cannot be converted to or from HJSON. */
class KSerialiserHJsonException : RuntimeException {
    constructor(message: String) : super(message)
}

/**
 * Converts Kotlin object graphs to and from HJSON.
 *
 * The serialiser is driven by a type model held in [registry]. Configure it with [configureFromTypeModel],
 * then register mappings for primitive types (e.g. [registerKotlinStdPrimitives]) and make the classes available
 * to `KotlinxReflect` so that objects can be created when reading.
 *
 * Composite properties are written in full; reference properties are written as `$ref` entries that point to the
 * path, from the root, of an object written elsewhere in the same document (e.g. `{ $ref : "#/child" }`).
 * An object met more than once (compared by identity) is written once in full and as a reference elsewhere.
 * Objects for which a registered lookup gives a key are written as `$LOOKUP` entries holding that key.
 *
 * Example:
 * ```
 * val serialiser = KSerialiserHJson()
 * serialiser.configureFromTypeModel(typeModel)
 * serialiser.registerKotlinStdPrimitives()
 * serialiser.registry.resolveImports()
 * val text = serialiser.toHJson(root, root).toHJsonString()
 * val copy: MyClass = serialiser.toData(text)
 * ```
 */
class KSerialiserHJson() {

    internal val reference_cache = mutableMapOf<Any, List<String>>()

    /** The type model and the primitive, lookup and other mappers used to walk and to re-create objects. */
    val registry = DatatypeRegistry()

    /** Used internally to stop walking the object graph once a reference target has been found. */
    class FoundReferenceException : RuntimeException {
        constructor() : super()
    }

    /**
     * Computes the path, from [root], of the object [targetValue], as written in the document.
     * Returns a path of the form `$unknown <ClassName>` if the object is not found.
     */
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
                collBegin { path, info, data, dt, et ->
                    WalkInfo(info.up, info.acc)
                }
                mapBegin { path, info, data, dt, kt, vt ->
                    WalkInfo(info.up, info.acc)
                }
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
                objectRepeat { path, info, obj, datatype, firstPath ->
                    // already walked (and so already written) at firstPath, written here as a reference
                    WalkInfo(info.up, info.acc)
                }
                lookup { path, info, obj, lookupName, key ->
                    // written by its lookup key, not walked
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

    //fun configureFromKompositeString(kompositeModel: String) {
    //TODO: mappers!
    //    registry.registerFromConfigString(kompositeModel, emptyMap())
    //}

    /**
     * Adds the types of the given type model to [registry].
     * The type model says, for each class, which properties are composite (`CMP`) or references (`REF`),
     * and which constructor parameters are used to create an instance when reading.
     */
    fun configureFromTypeModel(kompositeModel: TypesDomain) {
        //TODO: mappers!
        registry.registerFromTypeModel(kompositeModel, emptyMap())
    }

    /**
     * Registers the Kotlin standard library type model and the HJSON mappings for its primitive types.
     * `Boolean` and `String` are written as plain HJSON values; `Byte`, `Short`, `Int`, `Long`, `Float` and `Double`
     * are written as `$PRIMITIVE` objects so that their exact type is kept.
     */
    fun registerKotlinStdPrimitives() {
        this.registry.registerFromTypeModel(DatatypeRegistry.KOTLIN_STD_MODEL, emptyMap())
        this.registerPrimitive(Boolean::class, { value -> HJsonBoolean(value) }, { json -> json.asBoolean().value })
        this.registerPrimitiveAsObject(Byte::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toByte() })
        this.registerPrimitiveAsObject(Short::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toShort() })
        this.registerPrimitiveAsObject(Int::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toInt() })
        this.registerPrimitiveAsObject(Long::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toLong() })
        this.registerPrimitiveAsObject(Float::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toFloat() })
        this.registerPrimitiveAsObject(Double::class, { value -> HJsonNumber(value.toString()) }, { json -> json.asNumber().toDouble() })
        this.registerPrimitive(String::class, { value -> HJsonString(value) }, { json -> json.asString().value })
    }

    /**
     * Registers a mapping for the primitive type [cls], written as the raw HJSON value returned by `toHJson`.
     * The type must already be declared as a primitive in the type model.
     *
     * @throws KSerialiserHJsonException if [cls] is not defined in the type model
     */
    fun <T : Any> registerPrimitive(cls: KClass<T>, toHJson: (value: T) -> HJsonValue, toPrimitive: (json: HJsonValue) -> T) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findTypeDeclarationByKClass(cls) ?: throw KSerialiserHJsonException("The primitive is not defined in the Komposite configuration")
        this.registry.registerPrimitiveMapper(PrimitiveMapper.create(cls, HJsonValue::class, toHJson, toPrimitive))
    }

    /**
     * Registers a mapping for the primitive type [cls], written as a `$PRIMITIVE` object
     * with `$class` set to its qualified type name and `$value` set to the HJSON value returned by `toHJson`.
     * The type must already be declared as a primitive in the type model.
     *
     * @throws KSerialiserHJsonException if [cls] is not defined in the type model
     */
    fun <P : Any> registerPrimitiveAsObject(cls: KClass<P>, toHJson: (value: P) -> HJsonValue, fromHJson: (json: HJsonValue) -> P) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findTypeDeclarationByKClass(cls) ?: throw KSerialiserHJsonException("The primitive is not defined in the Komposite configuration")
        val toHJsonObj = { value: P ->
            val obj = HJsonUnreferencableObject()
            obj.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.PRIMITIVE.asHJsonString)
            obj.setProperty(HJsonDocument.CLASS, HJsonString(dt.qualifiedName.value))
            obj.setProperty(HJsonDocument.VALUE, toHJson(value))
            obj
        }
        val toPrimitive = { hjson: HJsonValue ->
            when (hjson) {
                is HJsonObject -> {
                    val value = hjson.property[HJsonDocument.VALUE]!!
                    fromHJson(value)
                }

                else -> fromHJson(hjson)
            }
        }
        val mapper = PrimitiveMapper.create(cls, HJsonObject::class, toHJsonObj, toPrimitive)
        this.registry.registerPrimitiveMapper(mapper)
    }

    /**
     * Converts [data] to a HJSON document.
     *
     * @param root the object that reference paths are computed from; normally the same as [data]
     * @param data the object to write
     * @return the HJSON document; use its string conversion functions to get the text
     */
    fun toHJson(root: Any, data: Any): HJsonDocument {
        println("*** root = $root (${root::class}")
        this.reference_cache.clear()
        val doc = HJsonDocument("json")
        val currentObjStack = mutableStackOf<HJsonValue>()
        val walker = kompositeWalker<List<String>, HJsonValue>(registry) {
            configure {
                ELEMENTS = HJsonDocument.ELEMENTS
                ENTRIES = HJsonDocument.ENTRIES
                KEY = HJsonDocument.KEY
                VALUE = HJsonDocument.VALUE
            }
            lookup { path, info, obj, lookupName, key ->
                // an object found, when read, by the key of a registered lookup (e.g. a definition owned by a singleton)
                val json = HJsonUnreferencableObject()
                json.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.LOOKUP.asHJsonString)
                json.setProperty(HJsonDocument.CLASS, HJsonString(lookupName))
                json.setProperty(HJsonDocument.VALUE, HJsonString(key))
                WalkInfo(info.up, json)
            }
            special { path, info, obj, type ->
                // a special value, e.g. kotlin.Unit (the agl value nothing), written as its agl type, e.g. std.Nothing
                val json = HJsonUnreferencableObject()
                json.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.SPECIAL.asHJsonString)
                json.setProperty(HJsonDocument.CLASS, HJsonString(type.qualifiedName.value))
                WalkInfo(info.up, json)
            }
            nullValue { path, info, dt ->
                WalkInfo(info.up, HJsonNull)
            }
            primitive { path, info, data, dt, mapper ->
                //TODO: use qualified name when/IF JS reflection make it possible!
                //println("*** found primitive ${primitive::class.simpleName!!} = $dt")
                val json = (mapper as PrimitiveMapper<Any, HJsonValue>?)?.toRaw?.invoke(data)
                    ?: throw KSerialiserHJsonException("Do not know how to convert ${data::class} to json, did you register its converter")
                WalkInfo(info.up, json)
            }
            valueType { path, info, data, dt, value, mapper ->
                val clsName = dt.valueProperty.typeInstance.typeName
                val json = (mapper as PrimitiveMapper<Any, HJsonValue>?)?.toRaw?.invoke(value)
                    ?: error("Do not know how to convert '${clsName}' to json, did you register its converter")
                val obj = HJsonUnreferencableObject()
                obj.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.ENUM.asHJsonString)
                obj.setProperty(HJsonDocument.CLASS, HJsonString(dt.qualifiedName.value))
                obj.setProperty(HJsonDocument.VALUE, json)
                WalkInfo(info.up, obj)
            }
            enum { path, info, data, dt ->
                val value = HJsonString(data.name)
                val obj = HJsonUnreferencableObject()
                obj.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.ENUM.asHJsonString)
                obj.setProperty(HJsonDocument.CLASS, HJsonString(dt.qualifiedName.value))
                obj.setProperty(HJsonDocument.VALUE, value)
                WalkInfo(info.up, obj)
            }
            reference { path, info, value, property ->
                val refPath = calcReferencePath(root, value)
                val ref = HJsonReference(doc, refPath)
                WalkInfo(path, ref)
            }
            objectRepeat { path, info, obj, datatype, firstPath ->
                // the same object (by identity) is only written once, other occurrences are written as a reference to it
                val ref = HJsonReference(doc, firstPath)
                WalkInfo(path, ref)
            }
            singleton { path, info, obj, datatype ->
                val json = HJsonReferencableObject(doc, path)
                reference_cache[obj] = json.path
                json.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.SINGLETON.asHJsonString)
                json.setProperty(HJsonDocument.CLASS, HJsonString(datatype.qualifiedName.value))
                WalkInfo(path, json)
            }
            collBegin { path, info, data, dt, et ->
                val elements = HJsonArray()
                currentObjStack.push(elements)
                WalkInfo(info.up, elements)
            }
            collElementEnd { path, info, element, et ->
                val elements = currentObjStack.peek() as HJsonArray
                elements.addElement(info.acc)
                //val nObj = listObj.withProperty(ELEMENTS, newList)
                //currentObjStack.push(nObj)
                WalkInfo(info.up, elements)
            }
            collEnd { path, info, data, dt, et ->
                val jsonTypeName = when (data) {
                //    is Array<*> -> HJsonDocument.ComplexObjectKind.ARRAY.asHJsonString
                    is Set<*> -> HJsonDocument.ComplexObjectKind.SET.asHJsonString
                    is List<*> -> HJsonDocument.ComplexObjectKind.LIST.asHJsonString
                    else -> throw KSerialiserHJsonException("Unknown type $dt")
                }
                val elements = currentObjStack.pop()
                val setObj = HJsonUnreferencableObject()
                setObj.setProperty(HJsonDocument.KIND, jsonTypeName)
                //ELEMENT_TYPE to HJsonString(type.elementType.qualifiedName), //needed for deserialising empty Arrays
                setObj.setProperty(HJsonDocument.ELEMENTS, elements)
                WalkInfo(info.up, setObj)
            }
            mapBegin { path, info, data, dt, kt, vt ->
                val obj = HJsonUnreferencableObject()
                obj.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.MAP.asHJsonString)
                obj.setProperty(HJsonDocument.ENTRIES, HJsonArray())
                currentObjStack.push(obj)
                WalkInfo(info.up, obj)
            }
            mapEntryKeyEnd { path, info, entry, kt, vt ->
                //push key ontostack
                currentObjStack.push(info.acc)
                info
            }
            mapEntryValueEnd { path, info, entry, kt, vt ->
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
            mapEnd { path, info, data, dt, kt, vt ->
                val obj = currentObjStack.pop()
                WalkInfo(info.up, obj)
            }
            objectBegin { path, info, obj, datatype ->
                val json = HJsonReferencableObject(doc, path)
                reference_cache[obj] = json.path
                json.setProperty(HJsonDocument.KIND, HJsonDocument.ComplexObjectKind.OBJECT.asHJsonString)
                json.setProperty(HJsonDocument.CLASS, HJsonString(datatype.qualifiedName.value))
                currentObjStack.push(json)
                WalkInfo(path, json)
            }
            objectEnd { path, info, obj, datatype ->
                val cuObj = currentObjStack.pop()
                WalkInfo(info.up, cuObj)
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

    /**
     * Parses [jsonString] as HJSON and re-creates the object it describes.
     * The class of each object is taken from its `$class` entry.
     *
     * @param targetKlass the expected class of the result, needed when the root is a `$ARRAY`
     * @return the re-created object, cast to [T]
     */
    fun <T : Any> toData(jsonString: String, targetKlass: KClass<T>? = null): T {
        //TODO: use a bespoke written JSON parser, it will most likely be faster
        val json = HJson.process(jsonString)
        val conv = FromHJsonConverter(this.registry)
        return conv.convertTo(emptyList(), json.root, targetKlass) as T
    }


}