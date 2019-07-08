package net.akehurst.kotlin.kserialisation.json

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.construct
import net.akehurst.kotlin.komposite.common.set


class FromJsonConverter(
        val registry : DatatypeRegistry
) {

    private val resolvedReference = mutableMapOf<String, Any>()

    fun convertValue(path:String, json: JsonValue): Any? {
        return when (json) {
            is JsonNull -> null
            is JsonString -> json.value
            is JsonNumber -> convertNumber(json)
            is JsonBoolean -> json.value
            is JsonArray -> convertList(path, json)
            is JsonObject -> convertObject(path, json)
            else -> throw KSerialiserJson.KSerialiserJsonException("Cannot convert $json")
        }
    }


    private fun convertNumber(json: JsonNumber): Number {
        return json.toDouble()
    }

    private fun convertObject(path:String, json: JsonObject) :Any {
        if (resolvedReference.containsKey(path)) {
            return resolvedReference[path]!!
        } else {
            val type = json.property[KSerialiserJson.TYPE]?.asString()?.value
            return when (type) {
                KSerialiserJson.LIST -> convertList(path, json.property[KSerialiserJson.ELEMENTS]!!.asArray())
                KSerialiserJson.SET -> convertSet(path, json.property[KSerialiserJson.ELEMENTS]!!.asArray())
                KSerialiserJson.OBJECT -> convertObject2(path, json)
                else -> {
                    convertObject2(path, json)
                }
            }
        }
    }

    private fun convertObject2(path:String, json: JsonObject) :Any {
        val clsName = json.property[KSerialiserJson.CLASS]!!.asString().value
        val ns = clsName.substringBeforeLast(".")
        val sn = clsName.substringAfterLast(".")
        //TODO: use ns
        val dt = registry.findDatatypeByName(sn)
        if (null==dt) {
            throw KSerialiserJson.KSerialiserJsonException("Cannot find datatype $clsName, is it in the datatype configuration")
        } else {
            val props = json.property.map {
                val value = this.convertValue(path + "/${it.key}", it.value)
                Pair(it.key, value)
            }.associate { it }
            val idProps = dt.identityProperties.map {
                props[it.name]
            }
            val obj = dt.construct(*idProps.toTypedArray())
            dt.nonIdentityProperties.forEach {
                it.set(obj, props[it.name])
            }
            return obj
        }
    }

    private fun convertList(path:String, json:JsonArray) : List<Any?> {
        return json.elements.mapIndexed { index, it ->
            this.convertValue(path+"/$index", it)
        }
    }
    private fun convertSet(path:String, json:JsonArray) : List<Any?> {
        return json.elements.mapIndexed { index, it ->
            this.convertValue(path+"/$index", it)
        }
    }
}