package net.akehurst.kotlin.kserialisation.json

class JsonException : RuntimeException {
    constructor(message:String) : super(message)
}

abstract class JsonValue {
    open fun asBoolean() : JsonBoolean {
        throw JsonException("Object is not a JsonNumber")
    }
    open fun asNumber() : JsonNumber {
        throw JsonException("Object is not a JsonNumber")
    }
    open fun asString() : JsonString {
        throw JsonException("Object is not a JsonString")
    }
    open fun asObject() : JsonObject {
        throw JsonException("Object is not a JsonObject")
    }
    open fun asReference() : JsonReference {
        throw JsonException("Object is not a JsonReference")
    }
    open fun asArray() : JsonArray {
        throw JsonException("Object is not a JsonArray")
    }

    abstract fun toJsonString() : String
}

data class JsonObject(
        val property: Map<String, JsonValue> = emptyMap()
) : JsonValue() {
    override fun asObject(): JsonObject {
        return this
    }

    fun withProperty(key:String, value:JsonValue) : JsonObject{
        val newProps = property + Pair(key,value)
        return JsonObject(newProps)
    }

    override fun toJsonString(): String {
        val elements = this.property.map {
            """"${it.key}":${it.value.toJsonString()}"""
        }.joinToString(",")
        return """{${elements}}"""
    }
}

data class JsonReference(
        val path:String
) : JsonValue() {
    override fun asReference(): JsonReference {
        return this
    }
    override fun toJsonString(): String {
        return """{ "${Json.REF}" : "$path" }"""
    }
}

data class JsonBoolean(
        val value: Boolean
) : JsonValue() {
    override fun asBoolean(): JsonBoolean {
        return this
    }
    override fun toJsonString(): String {
        return """${this.value}"""
    }
}

data class JsonNumber(
        private val _value:String
) : JsonValue() {

    fun toInt() : Int {
        return this._value.toInt()
    }

    fun toDouble() : Double {
        return this._value.toDouble()
    }

    override fun asNumber(): JsonNumber {
        return this
    }
    override fun toJsonString(): String {
        return """${this._value}"""
    }
}

data class JsonString(
        val value: String
) : JsonValue() {
    override fun asString(): JsonString {
        return this
    }
    override fun toJsonString(): String {
        return """"${this.value}""""
    }
}

data class JsonArray(
        val elements:List<JsonValue> = emptyList()
) : JsonValue() {
    override fun asArray(): JsonArray {
        return this
    }
    override fun toJsonString(): String {
        val elements = this.elements.map {
            it.toJsonString()
        }.joinToString(",")
        return """[${elements}]"""
    }

    fun withElement(element:JsonValue) : JsonArray {
        val newElements = this.elements + element
        return JsonArray(newElements)
    }
}

object JsonNull : JsonValue() {
    override fun toJsonString(): String {
        return "null"
    }
}