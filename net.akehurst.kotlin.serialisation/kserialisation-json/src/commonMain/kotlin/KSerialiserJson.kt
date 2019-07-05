package net.akehurst.kotlin.kserialisation.json

import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker


class KSerialiserJson(
        val datatypeModel: String
) {
    companion object {
        val ref = "${'$'}ref"
    }

    val registry = DatatypeRegistry()

    init {
        registry.registerFromConfigString(datatypeModel)
    }

    private fun calcPathTo(from: Any, to: Any?): String {
        return if (null == to) {
            ""
        } else {
            var path = ""
            val walker = kompositeWalker<String, Boolean>(registry) {
                objectBegin { info, obj, datatype ->
                    if (to == obj) {
                        WalkInfo(info.path, true)
                    } else {
                        WalkInfo(info.path, false)
                    }
                }
                propertyEnd { info, property ->
                    info
                }
            }
            walker.walk(WalkInfo("", true), from)
            return path;
        }
    }

    fun toJson(root: Any, data: Any): String {
        var result = ""
        val walker = kompositeWalker<Boolean, Boolean>(registry) {
            nullValue { info ->
                result += "null"
                info
            }
            primitive { info, value ->
                result += "${value.toString()}"
                info
            }
            reference { info, value, property ->
                val path = calcPathTo(root, value)
                result += """{ "$ref" : "$path" }"""
                info
            }
            listBegin { info, list ->
                result += "["
                info
            }
            listSeparate { info, list, previousElement ->
                result += ","
                info
            }
            listEnd { info, list ->
                result += "]"
                info
            }
            objectBegin { info, obj, datatype ->
                result += "{"
                info
            }
            objectEnd { info, obj, datatype ->
                result += "}"
                info
            }
            propertyBegin { info, property ->
                result += """"${property.name}" : """
                info
            }
            propertyEnd { info, property ->
                info
            }
        }

        walker.walk(WalkInfo(true, true), data)
        return result
    }


    fun toData(jsonRoot: String): Any {
        //TODO: use a bespoke written JSON parser, it will most likely be faster
        val json = Json.process(jsonRoot)
//        val walker
        TODO()
    }

}