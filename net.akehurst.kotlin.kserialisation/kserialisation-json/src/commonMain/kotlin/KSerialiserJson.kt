package net.akehurst.kotlin.kserialisation.json

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.PrimitiveType
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack
import kotlin.reflect.KClass


class KSerialiserJson() {

    class KSerialiserJsonException : RuntimeException {
        constructor(message: String) : super(message)
    }

    companion object {
        val TYPE = "${'$'}type"     // PRIMITIVE | OBJECT | LIST | SET | MAP
        val OBJECT = "${'$'}OBJECT"
        val CLASS = "${'$'}class"
        val PRIMITIVE = "${'$'}PRIMITIVE"
        val VALUE = "${'$'}value"
        val LIST = "${'$'}LIST"
        val MAP = "${'$'}MAP"
        val SET = "${'$'}SET"
        val ELEMENTS = "${'$'}elements"
    }

    private val reference_cache = mutableMapOf<Any, String>()
    private val primitiveToJson = mutableMapOf<PrimitiveType, (value:Any)->JsonValue>()
    private val primitiveFomJson = mutableMapOf<PrimitiveType, (value:JsonValue)->Any>()

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
    protected fun calcReferencePath(root:Any, targetValue:Any) : String {
        return if (reference_cache.containsKey(targetValue)) {
            reference_cache[targetValue]!!
        } else {
            val walker = kompositeWalker<List<String>, Boolean>(registry) {
                collBegin { key, info, coll ->
                    val path = info.path + key.toString()
                    WalkInfo(path, info.acc)
                }
                objectBegin { key, info, obj, datatype ->
                    val path = info.path + key.toString()
                    WalkInfo(path, obj==targetValue)
                }
                propertyBegin { key, info, property ->
                    val path = info.path + key.toString()
                    WalkInfo(path, info.acc)
                }
            }

            val result = walker.walk(WalkInfo(emptyList(), false), root)
            if (result.acc) result.path.joinToString("/") else "${'$'}unknown"
        }
    }

    fun confgureDatatypeModel(config:String) {
        registry.registerFromConfigString(config)
    }

    fun <T:Any> registerPrimitive(cls:KClass<T>, toJson:(value:T)->JsonValue, fromJson:(json:JsonValue)->T) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findPrimitiveByClass(cls) ?: throw KSerialiserJsonException("The primtive is not defined in the Komposite configuration")
        primitiveToJson[dt] = toJson as (Any)->JsonValue
        primitiveFomJson[dt] = fromJson
    }

    fun <T:Any> registerPrimitiveAsObject(cls:KClass<T>, toJson:(value:T)->JsonValue, fromJson:(json:JsonValue)->T) {
        //TODO: check cls is defined as primitive in the datatype registry..maybe auto add it!
        val dt = this.registry.findPrimitiveByClass(cls) ?: throw KSerialiserJsonException("The primtive is not defined in the Komposite configuration")
        primitiveToJson[dt] = { value:T ->
            JsonObject(mapOf(
                    KSerialiserJson.TYPE to JsonString(KSerialiserJson.PRIMITIVE),
                    KSerialiserJson.CLASS to JsonString(dt.qualifiedName(".")),
                    KSerialiserJson.VALUE to toJson(value)
            ))
        } as (Any)->JsonValue
        primitiveFomJson[dt] = { json ->
            val jsonValue = json.asObject().property[KSerialiserJson.VALUE]!!
            fromJson(jsonValue)
        }
    }

    fun toJson(root: Any, data: Any): String {
        var currentObjStack = Stack<JsonObject>()
        val walker = kompositeWalker<List<String>, JsonValue>(registry) {
            nullValue { key, info ->
                WalkInfo(info.path + key.toString(), JsonNull)
            }
            primitive { key, info, value ->
                //TODO: use qualified name when we can!
                val dt = registry.findPrimitiveByName(value::class.simpleName!!) ?: throw KSerialiserJsonException("The primtive is not defined in the Komposite configuration")
                val path = info.path + key.toString()
                val func = primitiveToJson[dt] ?: throw KSerialiserJsonException("Do not know how to convert ${value::class} to json, did you register its converter")
                val json = func(value)
                WalkInfo(path, json)
            }
            reference { key, info, value, property ->
                val path = info.path + key.toString()
                val refPath = calcReferencePath(root, value)
                val ref = JsonReference( refPath )
                WalkInfo(path, ref)
            }
            collBegin { key, info, coll ->
                val path = info.path + key.toString()
                val collTypeName = when(coll) {
                    is List<*> -> LIST
                    is Set<*> -> SET
                    else -> throw KSerialiserJsonException("Unknown collection type ${coll::class.simpleName}")
                }
                val listObj = JsonObject(mapOf(
                        "$TYPE" to JsonString(collTypeName),
                        "$ELEMENTS" to JsonArray()
                ))
                currentObjStack.push(listObj)
                WalkInfo(info.path, listObj)
            }
            collElementEnd { key, info, element ->
                val path = info.path + key.toString()
                val listObj = currentObjStack.pop()
                val list = (listObj.property[ELEMENTS] ?: JsonArray()) as JsonArray
                val newList = list.withElement(info.acc)
                val nObj = listObj.withProperty(ELEMENTS, newList)
                currentObjStack.push(nObj)
                WalkInfo(path, nObj)
            }
            collEnd { key, info, coll ->
                val listObj = currentObjStack.pop()
                WalkInfo(info.path, listObj)
            }
            mapBegin { key, info, map ->
                val path = info.path + key.toString()
                val listObj = JsonObject(mapOf(
                        "$TYPE" to JsonString(MAP),
                        "$ELEMENTS" to JsonObject()
                ))
                currentObjStack.push(listObj)
                WalkInfo(info.path, listObj)
            }
            mapEntryEnd { key, info, entry ->
                val path = info.path + key.toString()
                val mapObj = currentObjStack.pop()
                val map = (mapObj.property[ELEMENTS] ?: JsonObject()) as JsonObject
                val newMap = map.withProperty(key as String, info.acc)
                val nObj = mapObj.withProperty(ELEMENTS, newMap)
                currentObjStack.push(nObj)
                WalkInfo(path, nObj)
            }
            mapEnd { key, info, map ->
                val obj = currentObjStack.pop()
                WalkInfo(info.path, obj)
            }
            objectBegin { key, info, obj, datatype ->
                val path = info.path + key.toString()
                val obj = JsonObject(mutableMapOf(
                        "$TYPE" to JsonString(OBJECT),
                        "$CLASS" to JsonString(datatype.qualifiedName("."))
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
                val path = info.path + key.toString()
                val nObj = currentObjStack.pop().withProperty(key as String, info.acc)
                currentObjStack.push(nObj)
                WalkInfo(path, nObj)
            }
        }

        val result = walker.walk(WalkInfo(emptyList(), JsonNull), data)
        return result.acc.toJsonString()
    }


    fun toData(jsonRoot: String): Any? {
        //TODO: use a bespoke written JSON parser, it will most likely be faster
        val json = Json.process(jsonRoot)
        val conv = FromJsonConverter(this.registry, this.primitiveFomJson)
        return conv.convertValue("", json)
    }




}