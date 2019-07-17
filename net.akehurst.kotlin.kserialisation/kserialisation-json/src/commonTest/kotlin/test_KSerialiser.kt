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

package net.akehurst.kotlin.kserialisation.json

import com.soywiz.klock.DateTime
import net.akehurst.kotlinx.reflect.ModuleRegistry
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class A(
        val prop1: String
) {

    private var _privProp: Int = -1

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
            is A -> this.prop1 == other.prop1
            else -> false
        }
    }
}

class test_KSerialiser {


    val sut = KSerialiserJson()

    @BeforeTest
    fun setup() {
        this.sut.confgureDatatypeModel("""
            namespace com.soywiz.klock {
              primitive DateTime
            }
            namespace net.akehurst.kotlin.kserialisation.json {
    
                datatype A {
                    prop1 { identity(0) }
                }
            }
        """.trimIndent())

        this.sut.registerKotlinStdPrimitives()
        this.sut.registerPrimitiveAsObject(DateTime::class, //
                { value -> JsonNumber(value.unixMillisDouble.toString()) }, //
                { json -> DateTime.fromUnix(json.asNumber().toDouble()) }
        )
    }

    @Test
    fun toJson_Boolean_true() {

        val root = true

        val actual = this.sut.toJson(root, root)

        val expected = JsonBoolean(true).toJsonString()

        assertEquals(expected, actual)
    }

    @Test
    fun toData_Boolean_true() {

        val root = JsonBoolean(true).toJsonString()

        val actual = this.sut.toData(root)

        val expected = true

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Boolean_false() {

        val root = false

        val actual = this.sut.toJson(root, root)

        val expected = JsonBoolean(false).toJsonString()

        assertEquals(expected, actual)
    }

    @Test
    fun toData_Boolean_false() {

        val root = JsonBoolean(false).toJsonString()

        val actual = this.sut.toData(root)

        val expected = false

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_String() {

        val root = "hello"

        val actual = this.sut.toJson(root, root)

        val expected = JsonString("hello").toJsonString()

        assertEquals(expected, actual)
    }

    @Test
    fun toData_String() {

        val root = JsonString("hello").toJsonString()

        val actual = this.sut.toData(root)

        val expected = "hello"

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_DateTime() {

        val now = DateTime.now()

        val root = now

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findPrimitiveByName("DateTime")!!
        val expected = JsonObject(mapOf(
                KSerialiserJson.TYPE to JsonString(KSerialiserJson.PRIMITIVE),
                KSerialiserJson.CLASS to JsonString(dt.qualifiedName(".")),
                KSerialiserJson.VALUE to JsonNumber(now.unixMillisDouble.toString())
        )).toJsonString()

        assertEquals(expected, actual)
    }

    @Test
    fun toData_DateTime() {

        val now = DateTime.now()
        val dt = sut.registry.findPrimitiveByName("DateTime")!!
        val root = JsonObject(mapOf(
                KSerialiserJson.TYPE to JsonString(KSerialiserJson.PRIMITIVE),
                KSerialiserJson.CLASS to JsonString(dt.qualifiedName(".")),
                KSerialiserJson.VALUE to JsonNumber(now.unixMillisDouble.toString())
        )).toJsonString()

        val actual = this.sut.toData(root)

        val expected = now

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_List() {

        val root = listOf(1, true, "hello")

        val actual = this.sut.toJson(root, root)

        val expected = JsonObject(
                mapOf(
                        KSerialiserJson.TYPE to JsonString(KSerialiserJson.LIST),
                        KSerialiserJson.ELEMENTS to JsonArray(listOf(JsonNumber("1"), JsonBoolean(true), JsonString("hello")))
                )
        ).toJsonString()
        assertEquals(expected, actual)
    }

    @Test
    fun toData_List() {

        val root = JsonArray(listOf(JsonNumber("1"), JsonBoolean(true), JsonString("hello"))).toJsonString()

        val actual = this.sut.toData(root)

        val expected = listOf(1.0, true, "hello")

        assertEquals(expected, actual)
    }


    @Test
    fun toJson_Set() {

        val root = setOf(1, true, "hello")

        val actual = this.sut.toJson(root, root)

        val expected = JsonObject(
                mapOf(
                        KSerialiserJson.TYPE to JsonString(KSerialiserJson.SET),
                        KSerialiserJson.ELEMENTS to JsonArray(listOf(JsonNumber("1"), JsonBoolean(true), JsonString("hello")))
                )
        ).toJsonString()
        assertEquals(expected, actual)
    }

    @Test
    fun toData_Set() {

        val root = JsonObject(mapOf(
                KSerialiserJson.TYPE to JsonString(KSerialiserJson.SET),
                KSerialiserJson.ELEMENTS to JsonArray(listOf(JsonNumber("1"), JsonBoolean(true), JsonString("hello")))
        )).toJsonString()
        val actual = this.sut.toData(root)

        val expected = setOf(1.0, true, "hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Map() {

        val root = mapOf("a" to 1, "b" to true, "c" to "hello")

        val actual = this.sut.toJson(root, root)

        val expected = JsonObject(mapOf(
                KSerialiserJson.TYPE to JsonString(KSerialiserJson.MAP),
                KSerialiserJson.ELEMENTS to JsonArray(listOf(
                        JsonObject(mapOf(Pair(KSerialiserJson.KEY, JsonString("a")),Pair(KSerialiserJson.VALUE,JsonNumber("1")))),
                        JsonObject(mapOf(Pair(KSerialiserJson.KEY, JsonString("b")),Pair(KSerialiserJson.VALUE,JsonBoolean(true)))),
                        JsonObject(mapOf(Pair(KSerialiserJson.KEY, JsonString("c")),Pair(KSerialiserJson.VALUE,JsonString("hello"))))
                )))).toJsonString()
        assertEquals(expected, actual)
    }

    @Test
    fun toData_Map() {

        val root = JsonObject(mapOf(
                KSerialiserJson.TYPE to JsonString(KSerialiserJson.MAP),
                KSerialiserJson.ELEMENTS to JsonArray(listOf(
                        JsonObject(mapOf(Pair(KSerialiserJson.KEY, JsonString("a")),Pair(KSerialiserJson.VALUE,JsonNumber("1")))),
                        JsonObject(mapOf(Pair(KSerialiserJson.KEY, JsonString("b")),Pair(KSerialiserJson.VALUE,JsonBoolean(true)))),
                        JsonObject(mapOf(Pair(KSerialiserJson.KEY, JsonString("c")),Pair(KSerialiserJson.VALUE,JsonString("hello"))))
                )))).toJsonString()

        val actual = this.sut.toData(root)

        val expected = mapOf("a" to 1.0, "b" to true, "c" to "hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_A() {

        val root = A("hello")
        root.setProp2(5)
        val dtA = sut.registry.findDatatypeByName("A")!!

        val actual = this.sut.toJson(root, root)

        val expected = JsonObject(mapOf(
                KSerialiserJson.TYPE to JsonString(KSerialiserJson.OBJECT),
                KSerialiserJson.CLASS to JsonString(dtA.qualifiedName(".")),
                "prop1" to JsonString("hello"),
                "prop2" to JsonNumber("5")
        )).toJsonString()

        assertEquals(expected, actual)
    }

    @Test
    fun toData_A() {
        sut.registerModule("net.akehurst.kotlin.kserialisation-kserialisation-json-test")
        val dtA = sut.registry.findDatatypeByName("A")!!

        val json = JsonObject(mapOf(
                KSerialiserJson.CLASS to JsonString(dtA.qualifiedName(".")),
                "prop1" to JsonString("hello")
        )).toJsonString()

        val actual = this.sut.toData(json)

        val expected = A("hello")

        assertEquals(expected, actual)
    }

}