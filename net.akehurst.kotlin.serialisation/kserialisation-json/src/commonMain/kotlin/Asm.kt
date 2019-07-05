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
    open fun asArray() : JsonArray {
        throw JsonException("Object is not a JsonArray")
    }
}

data class JsonObject(
        val property: Map<String, JsonValue>
) : JsonValue() {
    override fun asObject(): JsonObject {
        return this
    }

}

data class JsonBoolean(
        val value: Boolean
) : JsonValue() {
    override fun asBoolean(): JsonBoolean {
        return this
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
}

data class JsonString(
        val value: String
) : JsonValue() {
    override fun asString(): JsonString {
        return this
    }
}

data class JsonArray(
        val value:List<JsonValue>
) : JsonValue() {
    override fun asArray(): JsonArray {
        return this
    }
}

object JsonNull : JsonValue()