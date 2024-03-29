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

package net.akehurst.kotlin.kserialisation.bytes


import korlibs.time.DateTime
import net.akehurst.kotlin.komposite.processor.komposite
import net.akehurst.kotlin.kserialisation.json.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AAA(
        val prop1: String
) {

    private var _privProp: Int = -1

    var comp: AAA? = null
    var refr: AAA? = null

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
            is AAA -> this.prop1 == other.prop1
            else -> false
        }
    }
}

class test_KSerialiser {


    val sut = KSerialiserBytes()

    @BeforeTest
    fun setup() {
        this.sut.confgureFromKompositeString("""
            namespace com.soywiz.klock {
              primitive DateTime
            }
            namespace net.akehurst.kotlin.kserialisation.json {
                datatype AAA {
                    val prop1 : String
                    car comp : AAA
                    var refr : AAA
                    var prop2  : Int
                }
            }
        """.trimIndent())

        this.sut.confgureFromKompositeModel(komposite {
            namespace("korlibs.time") {
                primitiveType("DateTime")
            }
            namespace("net.akehurst.kotlin.kserialisation.bytes") {
                dataType("AAA") {

                }
            }
        })

        this.sut.registerKotlinStdLib()
        this.sut.registerPrimitiveAsObject(DateTime::class, //
                { value -> JsonNumber(value.unixMillisDouble.toString()) }, //
                { json -> DateTime.fromUnixMillis(json.asNumber().toDouble()) }
        )
        //ModuleRegistry.registerClass("net.akehurst.kotlin.kserialisation.json.AAA",AAA::class)
        //sut.registerModule("net.akehurst.kotlin.kserialisation-kserialisation-json-test")
    }

    @Test
    fun toJson_Boolean_true() {

        val root = true

        val actual = this.sut.toJson(root, root)

        val expected = JsonBoolean(true).toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_Boolean_true() {

        val root = JsonBoolean(true).toFormattedJsonString("  ", "  ")

        val actual:Boolean = this.sut.toData(root)

        val expected = true

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Boolean_false() {

        val root = false

        val actual = this.sut.toJson(root, root)

        val expected = JsonBoolean(false).toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_Boolean_false() {

        val root = JsonBoolean(false).toFormattedJsonString("  ", "  ")

        val actual:Boolean = this.sut.toData(root)

        val expected = false

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Byte_1() {

        val root = 1.toByte()
        assertEquals("Byte", root::class.simpleName)

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findPrimitiveByName("Byte")!!
        assertEquals("Byte", dt.name)

        val expected = json("expected") {
            primitiveObject(dt.qualifiedNameBy("."), root)
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_Byte_1() {

        val dt = sut.registry.findPrimitiveByName("Byte")!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedNameBy("."), 1)
        }.toFormattedJsonString("  ", "  ")

        val actual:Byte = this.sut.toData(root)

        val expected = 1.toByte()

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Int_1() {

        val root = 1.toInt()

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findPrimitiveByName("Int")!!
        val expected = json("expected") {
            primitiveObject(dt.qualifiedNameBy("."), root)
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_Int_1() {

        val dt = sut.registry.findPrimitiveByName("Int")!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedNameBy("."), 1)
        }.toFormattedJsonString("  ", "  ")

        val actual:Int = this.sut.toData(root)

        val expected = 1.toInt()

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Long_1() {

        val root = 1.toLong()

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findPrimitiveByName("Long")!!
        val expected = json("expected") {
            primitiveObject(dt.qualifiedNameBy("."), root)
        }.toFormattedJsonString("  ", "  ")


        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_Long_1() {

        val dt = sut.registry.findPrimitiveByName("Long")!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedNameBy("."), 1)
        }.toFormattedJsonString("  ", "  ")

        val actual:Long = this.sut.toData(root)

        val expected = 1.toLong()

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_String() {

        val root = "hello world!"

        val actual = this.sut.toJson(root, root)

        val expected = JsonString("hello world!").toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_String() {

        val root = JsonString("hello").toFormattedJsonString("  ", "  ")

        val actual:String = this.sut.toData(root)

        val expected = "hello"

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_String_with_quotes() {

        val root = "hello \"world!\""

        val actual = this.sut.toJson(root, root)

        val expected = JsonString("hello \\\"world!\\\"").toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_String_with_quotes() {

        val root = JsonString("hello \\\"world!\\\"").toFormattedJsonString("  ", "  ")

        val actual:String = this.sut.toData(root)

        val expected = "hello \"world!\""

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_String_with_linebreak() {

        val root = """
            hello
            world!
        """.trimIndent()

        val actual = this.sut.toJson(root, root)

        val expected = "\"hello\\nworld!\""

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_String_with_linebreak() {

        val root = "\"hello\\nworld!\""

        val actual:String = this.sut.toData(root)

        val expected = """
            hello
            world!
        """.trimIndent()

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_DateTime() {

        val now = DateTime.now()
        val root = now

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findPrimitiveByName("DateTime")!!

        val expected = json("expected") {
            primitiveObject(dt.qualifiedNameBy("."), now.unixMillisDouble)
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_DateTime() {

        val now = DateTime.now()
        val dt = sut.registry.findPrimitiveByName("DateTime")!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedNameBy("."), now.unixMillisDouble)
        }.toFormattedJsonString("  ", "  ")

        val actual:DateTime = this.sut.toData(root)

        val expected = now

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Array_empty_Int() {

        val root = emptyArray<Int>()

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            arrayObject {

            }
        }.toFormattedJsonString("  ", "  ")
        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_Array_empty_Int() {

        val root = json("expected") {
            arrayObject {

            }
        }.toFormattedJsonString("  ", "  ")

        val actual = this.sut.toData(root) as Array<Any>

        val expected = emptyArray<Int>()

        assertTrue(expected contentEquals actual)
    }

    @Test
    fun toJson_Array() {

        val root = arrayOf(1, true, "hello")

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            arrayObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_Array() {

        val root = json("expected") {
            arrayObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toFormattedJsonString("  ", "  ")

        val actual = this.sut.toData(root) as Array<Any>

        val expected = arrayOf(1, true, "hello")

        assertTrue(expected contentEquals actual)
    }

    @Test
    fun toJson_List() {

        val root = listOf(1, true, "hello")

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            listObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_List() {

        val root = json("expected") {
            listObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toFormattedJsonString("  ", "  ")

        val actual:List<Any> = this.sut.toData(root)

        val expected = listOf(1, true, "hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Set() {

        val root = setOf(1, true, "hello")

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            setObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_Set() {

        val root = json("expected") {
            setObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toFormattedJsonString("  ", "  ")

        val actual:Set<Any> = this.sut.toData(root)

        val expected = setOf(1, true, "hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Map() {

        val root = mapOf("a" to 1, "b" to true, "c" to "hello")

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            mapObject {
                entry({ string("a") }) { primitiveObject("kotlin.Int", 1) }
                entry({ string("b") }) { boolean(true) }
                entry({ string("c") }) { string("hello") }
            }
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_Map() {

        val root = json("expected") {
            mapObject {
                entry({ string("a") }) { primitiveObject("kotlin.Int", 1) }
                entry("b", true)
                entry("c", "hello")
            }
        }.toFormattedJsonString("  ", "  ")

        val actual:Map<String,Any> = this.sut.toData(root)

        val expected = mapOf("a" to 1, "b" to true, "c" to "hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_A() {

        val root = AAA("1: hello")
        root.setProp2(5)
        val dtA = sut.registry.findDatatypeByName("AAA")!!

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            objectReferenceable(dtA.qualifiedNameBy(".")) {
                property("comp", null)
                property("prop1", "1: hello")
                property("refr", null)
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_A() {

        val dtA = sut.registry.findDatatypeByName("AAA")!!

        val json = json("expected") {
            objectReferenceable(dtA.qualifiedNameBy(".")) {
                property("prop1", "hello")
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toFormattedJsonString("  ", "  ")

        val actual:AAA = this.sut.toData(json)

        val expected = AAA("hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_A_2() {

        val root = AAA("1: hello")
        root.setProp2(5)
        root.comp = AAA("1.3")
        root.comp?.refr = root
        val dtA = sut.registry.findDatatypeByName("AAA")!!

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            objectReferenceable(dtA.qualifiedNameBy(".")) {
                property("comp") {
                    objectReferenceable(dtA.qualifiedNameBy(".")) {
                        property("comp", null)
                        property("prop1", "1.3")
                        property("refr") {
                            reference("/")
                        }
                        property("prop2") {
                            primitiveObject("kotlin.Int", -1)
                        }
                    }
                }
                property("prop1", "1: hello")
                property("refr", null)
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ","  "))
    }

    @Test
    fun toData_A_2() {

        val dtA = sut.registry.findDatatypeByName("AAA")!!

        val json = json("expected") {
            objectReferenceable(dtA.qualifiedNameBy(".")) {
                property("prop1", "1: hello")
                property("comp") {
                    objectReferenceable(dtA.qualifiedNameBy(".")) {
                        property("prop1", "1.3")
                        property("prop3", null)
                        property("refr") {
                            reference("/")
                        }
                        property("prop2") {
                            primitiveObject("kotlin.Int", -1)
                        }
                    }
                }
                property("prop4", null)
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toFormattedJsonString("  ", "  ")

        val actual:AAA = this.sut.toData(json)

        val expected = AAA("1: hello")
        expected.setProp2(5)
        expected.comp = AAA("1.3")
        expected.comp?.refr = expected

        assertEquals(expected, actual)
        assertEquals(expected.getProp2(), actual.getProp2())
        assertEquals(expected.comp, actual.comp)
        assertEquals(expected.comp?.getProp2(), actual.comp?.getProp2())
        assertEquals(expected.comp?.comp, actual.comp?.comp)
        assertEquals(expected.comp?.refr, actual.comp?.refr)
        assertEquals(expected.refr, actual.refr)
    }


}