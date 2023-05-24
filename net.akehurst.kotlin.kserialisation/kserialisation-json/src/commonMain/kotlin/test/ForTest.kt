package net.akehurst.kotlin.kserialisation.json.test

enum class TestEnumEEE { RED, GREEN, BLUE }

class TestClassAAA(
    val prop1: String
) {

    private var _privProp: Int = -1

    var comp: TestClassAAA? = null
    var refr: TestClassAAA? = null

    fun getProp2(): Int {
        return this._privProp
    }

    fun setProp2(value: Int) {
        this._privProp = value
    }

    override fun hashCode(): Int {
        return prop1.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is TestClassAAA -> this.prop1 == other.prop1
            else -> false
        }
    }
}