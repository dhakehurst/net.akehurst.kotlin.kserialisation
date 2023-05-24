package net.akehurst.kotlin.kserialisation.json.test

/*
    For the reflection to work correctly, the classes need to be 'exported'
    the 'exportPublic' plugin does not export things from 'test' modules.
    neither does the annotation
    ...bleh!
 */

enum class TestEnumEEE { RED, GREEN, BLUE }

class TestClassAAA(
    val prop1: String,
    var comp: TestClassAAA?
) {

    private var _privProp: Int = -1

    var refr: TestClassAAA? = null

    var prop2: Int
        get() = this._privProp
        set(value: Int) {
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