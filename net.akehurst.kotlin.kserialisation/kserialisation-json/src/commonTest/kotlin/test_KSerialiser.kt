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


import korlibs.time.DateTime
import net.akehurst.kotlin.json.*
import net.akehurst.kotlin.komposite.common.clazz
import net.akehurst.kotlin.kserialisation.json.test.TestClassAAA
import net.akehurst.kotlin.kserialisation.json.test.TestEnumEEE
import net.akehurst.kotlinx.reflect.EnumValuesFunction
import net.akehurst.kotlinx.reflect.KotlinxReflect
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class test_KSerialiser {

    private val sut = KSerialiserJson()

    @BeforeTest
    fun setup() {
        this.sut.configureFromKompositeString(
            """
            namespace korlibs.time {
              primitive DateTime
            }
            namespace net.akehurst.kotlin.kserialisation.json.test {
                import kotlin
                enum TestEnumEEE
                datatype TestClassAAA {
                    composite-val prop1 : String
                    composite-val comp : TestClassAAA?
                    reference-var refr : TestClassAAA?
                    reference-var prop2  : Int
                }
            }
        """.trimIndent()
        )

        this.sut.registerKotlinStdPrimitives()
        this.sut.registerPrimitiveAsObject(DateTime::class, //
            { value -> JsonNumber(value.unixMillisDouble.toString()) }, //
            { json -> DateTime.fromUnixMillis(json.asNumber().toDouble()) }
        )
        KotlinxReflect.registerClass("net.akehurst.kotlin.kserialisation.json.test.TestClassAAA", TestClassAAA::class)
        KotlinxReflect.registerClass("net.akehurst.kotlin.kserialisation.json.test.TestEnumEEE", TestEnumEEE::class, TestEnumEEE::values as EnumValuesFunction)
        this.sut.registry.resolveImports()
    }

    @Test
    fun toJson_Boolean_true() {

        val root = true

        val actual = this.sut.toJson(root, root)

        val expected = JsonBoolean(true).toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Boolean_true() {

        val root = JsonBoolean(true).toFormattedJsonString("  ", "  ")

        val actual: Boolean = this.sut.toData(root)

        val expected = true

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Boolean_false() {

        val root = false

        val actual = this.sut.toJson(root, root)

        val expected = JsonBoolean(false).toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Boolean_false() {

        val root = JsonBoolean(false).toFormattedJsonString("  ", "  ")

        val actual: Boolean = this.sut.toData(root)

        val expected = false

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Byte_1() {

        val root:Any = 1.toByte()
        println("class.simpleName: ${root::class.simpleName}")
        assertEquals("Byte", root::class.simpleName)

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findFirstByNameOrNull("Byte")!!
        assertEquals("Byte", dt.name)

        val expected = json("expected") {
            primitiveObject(dt.qualifiedName, root)
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Byte_1() {

        val dt = sut.registry.findFirstByNameOrNull("Byte")!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedName, 1)
        }.toFormattedJsonString("  ", "  ")

        val actual: Byte = this.sut.toData(root)

        val expected = 1.toByte()

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Int_1() {

        val root = 1.toInt()

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findFirstByNameOrNull("Int")!!
        val expected = json("expected") {
            primitiveObject(dt.qualifiedName, root)
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Int_1() {

        val dt = sut.registry.findFirstByNameOrNull("Int")!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedName, 1)
        }.toFormattedJsonString("  ", "  ")

        val actual: Int = this.sut.toData(root)

        val expected = 1.toInt()

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Long_1() {

        val root = 1.toLong()

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findFirstByNameOrNull("Long")!!
        val expected = json("expected") {
            primitiveObject(dt.qualifiedName, root)
        }.toFormattedJsonString("  ", "  ")


        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Long_1() {

        val dt = sut.registry.findFirstByNameOrNull("Long")!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedName, 1)
        }.toFormattedJsonString("  ", "  ")

        val actual: Long = this.sut.toData(root)

        val expected = 1.toLong()

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_String() {

        val root = "hello world!"

        val actual = this.sut.toJson(root, root)

        val expected = JsonString("hello world!").toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_String() {

        val root = JsonString("hello").toFormattedJsonString("  ", "  ")

        val actual: String = this.sut.toData(root)

        val expected = "hello"

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_String_with_quotes() {

        val root = "hello \"world!\""

        val actual = this.sut.toJson(root, root)

        val expected = "\"hello \\\"world!\\\"\""

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_String_with_quotes() {

        val root = JsonString("hello \"world!\"").toFormattedJsonString("  ", "  ")

        val actual: String = this.sut.toData(root)

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

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_String_with_linebreak() {

        val root = "\"hello\\nworld!\""

        val actual: String = this.sut.toData(root)

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

        val dt = sut.registry.findFirstByNameOrNull("DateTime")!!

        val expected = json("expected") {
            primitiveObject(dt.qualifiedName, now.unixMillisDouble)
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_DateTime() {

        val now = DateTime.now()
        val dt = sut.registry.findFirstByNameOrNull("DateTime")!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedName, now.unixMillisDouble)
        }.toFormattedJsonString("  ", "  ")

        val actual: DateTime = this.sut.toData(root)

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
        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Array_empty_Int() {

        val root = json("expected") {
            arrayObject {

            }
        }.toFormattedJsonString("  ", "  ")

        val actual = this.sut.toData(root, Array::class) as Array<Any>

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

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
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

        val actual = this.sut.toData(root, Array::class) as Array<Any>

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

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
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

        val actual: List<Any> = this.sut.toData(root)

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

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
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

        val actual: Set<Any> = this.sut.toData(root)

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

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
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

        val actual: Map<String, Any> = this.sut.toData(root)

        val expected = mapOf("a" to 1, "b" to true, "c" to "hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Enum() {
        val root = TestEnumEEE.RED
        val dtE = sut.registry.findTypeDeclarationByKClass(TestEnumEEE::class)

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            enumObject("net.akehurst.kotlin.kserialisation.json.test.TestEnumEEE", TestEnumEEE.RED)
        }.toFormattedJsonString("  ", "  ")
        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Enum() {
        val dtE = sut.registry.findTypeDeclarationByKClass(TestEnumEEE::class)
        val json = json("expected") {
            enumObject("net.akehurst.kotlin.kserialisation.json.test.TestEnumEEE", TestEnumEEE.RED)
        }.toStringJson()

        val actual: TestEnumEEE = this.sut.toData(json)

        val expected = TestEnumEEE.RED

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_A() {

        val root = TestClassAAA("1: hello",null)
        root.prop2 = (5)
        val dtA = sut.registry.findFirstByNameOrNull("TestClassAAA")!!

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            objectReferenceable(dtA.qualifiedName) {
                property("prop1", "1: hello")
                property("comp", null)
                property("refr", null)
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_A() {

        val dtA = sut.registry.findFirstByNameOrNull("TestClassAAA")!!

        val json = json("expected") {
            objectReferenceable(dtA.qualifiedName) {
                property("prop1", "hello")
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toFormattedJsonString("  ", "  ")

        val actual: TestClassAAA = this.sut.toData(json)

        val expected = TestClassAAA("hello",null)

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_A_2() {

        val root = TestClassAAA("1: hello",TestClassAAA("1.3",null))
        root.prop2 = (5)
        root.comp?.refr = root
        val dtA = sut.registry.findFirstByNameOrNull("TestClassAAA")!!

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            objectReferenceable(dtA.qualifiedName) {
                property("prop1", "1: hello")
                property("comp") {
                    objectReferenceable(dtA.qualifiedName) {
                        property("prop1", "1.3")
                        property("comp", null)
                        property("refr") {
                            reference("/")
                        }
                        property("prop2") {
                            primitiveObject("kotlin.Int", -1)
                        }
                    }
                }
                property("refr", null)
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_A_2() {
        //FIXME: this does not work in JS tests because the getters/setters are not included as properties by kotlinx-reflect!
        val dtA = sut.registry.findFirstByNameOrNull("TestClassAAA")!!
        println("dta = $dtA")
        println(KotlinxReflect.registeredClasses)
        println("dta = ${dtA.clazz}")

        val json = json("expected") {
            objectReferenceable(dtA.qualifiedName) {
                property("prop1", "1: hello")
                property("comp") {
                    objectReferenceable(dtA.qualifiedName) {
                        property("prop1", "1.3")
                        property("comp",null)
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

        val actual: TestClassAAA = this.sut.toData(json)

        val expected = TestClassAAA("1: hello",TestClassAAA("1.3",null))
        expected.prop2 = (5)
        expected.comp?.refr = expected

        assertEquals(expected, actual)
        assertEquals(expected.prop2, actual.prop2)
        assertEquals(expected.comp, actual.comp)
        assertEquals(expected.comp?.prop2, actual.comp?.prop2)
        assertEquals(expected.comp?.comp, actual.comp?.comp)
        assertEquals(expected.comp?.refr, actual.comp?.refr)
        assertEquals(expected.refr, actual.refr)
    }
}