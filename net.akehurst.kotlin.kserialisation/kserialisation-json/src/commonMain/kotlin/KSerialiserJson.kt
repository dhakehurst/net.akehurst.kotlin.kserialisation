package net.akehurst.kotlin.kserialisation.json

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack


class KSerialiserJson(
        val datatypeModel: String
) {

    class KSerialiserJsonException : RuntimeException {
        constructor(message: String) : super(message)
    }

    companion object {
        val CLASS = "${'$'}class"
        val TYPE = "${'$'}type"
        val ELEMENTS = "${'$'}elements"
        val OBJECT = "${'$'}OBJECT"
        val LIST = "${'$'}LIST"
        val SET = "${'$'}SET"
    }

    private val reference_cache = mutableMapOf<Any, String>()

    val registry = DatatypeRegistry()
    val jsonReg = DatatypeRegistry()

    init {
        registry.registerFromConfigString(datatypeModel)
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

    fun toJson(root: Any, data: Any): String {
        var currentObjStack = Stack<JsonObject>()
        val walker = kompositeWalker<List<String>, JsonValue>(registry) {
            nullValue { key, info ->
                WalkInfo(info.path + key.toString(), JsonNull)
            }
            primitive { key, info, value ->
                val path = info.path + key.toString()
                when (value) {
                    is String -> WalkInfo(path, JsonString(value))
                    is Boolean -> WalkInfo(path, JsonBoolean(value))
                    is Int -> WalkInfo(path, JsonNumber(value.toString()))
                    is Long -> WalkInfo(path, JsonNumber(value.toString()))
                    is Float -> WalkInfo(path, JsonNumber(value.toString()))
                    is Double -> WalkInfo(path, JsonNumber(value.toString()))
                    else -> throw KSerialiserJsonException("Do not know how to handle primitive of type ${value::class.simpleName}")
                }
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
                        "$ELEMENTS" to JsonArray(mutableListOf())
                ))
                currentObjStack.push(listObj)
                WalkInfo(info.path, listObj)
            }
            collSeparate { key, info, coll, previousElement ->
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
                val nObj = currentObjStack.pop().withProperty(property.name, info.acc)
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
        val conv = FromJsonConverter(this.registry)
        return conv.convertValue("", json)
    }




}