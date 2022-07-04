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

package net.akehurst.kotlin.kserialisation.hjson

import com.soywiz.klock.DateTime
import net.akehurst.hjson.*
import net.akehurst.kotlinx.reflect.KotlinxReflect
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TestClassAAA(
    internal val prop1: String
) {

    private var _privProp: Int = -1

    internal var comp: TestClassAAA? = null
    internal var refr: TestClassAAA? = null

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

class test_KSerialiser {


    val sut = KSerialiserHJson()

    @BeforeTest
    fun setup() {
        this.sut.confgureFromKompositeString("""
            namespace com.soywiz.klock {
              primitive DateTime
            }
            namespace net.akehurst.kotlin.kserialisation.hjson {
                datatype TestClassAAA {
                    composite-val prop1 : String
                    composite-var comp : A
                    reference-var refr : A
                    composite-var prop2  : Int
                }
            }
        """.trimIndent())

        this.sut.registerKotlinStdPrimitives()
        this.sut.registerPrimitiveAsObject(DateTime::class, //
                { value -> HJsonNumber(value.unixMillisDouble.toString()) }, //
                { json -> DateTime.fromUnix(json.asNumber().toDouble()) }
        )
        KotlinxReflect.registerClass("net.akehurst.kotlin.kserialisation.hjson.TestClassAAA",TestClassAAA::class)
    }

    @Test
    fun toHJson_Boolean_true() {

        val root = true

        val actual = this.sut.toHJson(root, root)

        val expected = HJsonBoolean(true).toHJsonString("","")

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_Boolean_true() {

        val root = HJsonBoolean(true).toHJsonString("","")

        val actual:Boolean = this.sut.toData(root)

        val expected = true

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_Boolean_false() {

        val root = false

        val actual = this.sut.toHJson(root, root)

        val expected = HJsonBoolean(false).toHJsonString("","")

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_Boolean_false() {

        val root = HJsonBoolean(false).toHJsonString("","")

        val actual:Boolean = this.sut.toData(root)

        val expected = false

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_Byte_1() {

        assertEquals("Byte", Byte::class.simpleName)
        assertEquals("kotlin.Byte", sut.registry.findPrimitiveByClass(Byte::class)?.qualifiedName)

        val root:Byte = 1.toByte()
        assertEquals("Byte", root::class.simpleName)

        val x:Any = root
        assertEquals("Byte", x::class.simpleName) //This needs to pass or rest of test will fail

        val actual = this.sut.toHJson(root, root)

        val dt = sut.registry.findPrimitiveByName("Byte")!!
        assertEquals("Byte", dt.name)

        val expected = hjson("expected") {
            primitiveObject(dt.qualifiedName, root)
        }.toHJsonString()

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_Byte_1() {

        val dt = sut.registry.findPrimitiveByName("Byte")!!
        val root = hjson("expected") {
            primitiveObject(dt.qualifiedName, 1)
        }.toHJsonString()

        val actual:Byte = this.sut.toData(root)

        val expected = 1.toByte()

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_Int_1() {

        val root = 1.toInt()

        val actual = this.sut.toHJson(root, root)

        val dt = sut.registry.findPrimitiveByName("Int")!!
        val expected = hjson("expected") {
            primitiveObject(dt.qualifiedName, root)
        }.toHJsonString()

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_Int_1() {

        val dt = sut.registry.findPrimitiveByName("Int")!!
        val root = hjson("expected") {
            primitiveObject(dt.qualifiedName, 1)
        }.toHJsonString()

        val actual:Int = this.sut.toData(root)

        val expected = 1.toInt()

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_Long_1() {

        val root = 1.toLong()

        val actual = this.sut.toHJson(root, root)

        val dt = sut.registry.findPrimitiveByName("Long")!!
        val expected = hjson("expected") {
            primitiveObject(dt.qualifiedName, root)
        }.toHJsonString()


        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_Long_1() {

        val dt = sut.registry.findPrimitiveByName("Long")!!
        val root = hjson("expected") {
            primitiveObject(dt.qualifiedName, 1)
        }.toHJsonString()

        val actual:Long = this.sut.toData(root)

        val expected = 1.toLong()

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_String() {

        val root = "hello world!"

        val actual = this.sut.toHJson(root, root)

        val expected = HJsonString("hello world!").toHJsonString("","")

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_String() {

        val root = "\"hello\""

        val actual:String = this.sut.toData(root)

        val expected = "hello"

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_String_with_quotes() {

        val root = "hello \"world!\""

        val actual = this.sut.toHJson(root, root)

        val expected = "hello \\\"world!\\\""

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_String_with_quotes() {

        val root = "\"hello \\\"world!\\\"\""

        val actual:String = this.sut.toData(root)

        val expected = "hello \"world!\""

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_String_with_linebreak() {

        val root = """
            hello
            world!
        """.trimIndent()

        val actual = this.sut.toHJson(root, root)

        val expected = "'''hello\nworld!'''"

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_String_with_linebreak() {

        val root = "'''hello\nworld!'''"

        val actual:String = this.sut.toData(root)

        val expected = """
            hello
            world!
        """.trimIndent()

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_DateTime() {

        val now = DateTime.now()
        val root = now

        val actual = this.sut.toHJson(root, root)

        val dt = sut.registry.findPrimitiveByName("DateTime")!!

        val expected = hjson("expected") {
            primitiveObject(dt.qualifiedName, now.unixMillisDouble)
        }.toHJsonString()

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_DateTime() {

        val now = DateTime.now()
        val dt = sut.registry.findPrimitiveByName("DateTime")!!
        val root = hjson("expected") {
            primitiveObject(dt.qualifiedName, now.unixMillisDouble)
        }.toHJsonString()

        val actual:DateTime = this.sut.toData(root)

        val expected = now

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_Array_empty_Int() {

        val root = emptyArray<Int>()

        val actual = this.sut.toHJson(root, root)

        val expected = hjson("expected") {
            arrayObject {

            }
        }.toHJsonString()
        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_Array_empty_Int() {

        val root = hjson("expected") {
            arrayObject {

            }
        }.toHJsonString()

        val actual = this.sut.toData(root) as Array<Any>

        val expected = emptyArray<Int>()

        assertTrue(expected contentEquals actual)
    }

    @Test
    fun toHJson_Array() {

        val root = arrayOf(1, true, "hello")

        val actual = this.sut.toHJson(root, root)

        val expected = hjson("expected") {
            arrayObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toHJsonString()

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_Array() {

        val root = hjson("expected") {
            arrayObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toHJsonString()

        val actual = this.sut.toData(root) as Array<Any>

        val expected = arrayOf(1, true, "hello")

        assertTrue(expected contentEquals actual)
    }


    @Test
    fun toHJson_List() {

        val root = listOf(1, true, "hello")

        val actual = this.sut.toHJson(root, root)

        val expected = hjson("expected") {
            listObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toHJsonString()

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_List() {

        val root = hjson("expected") {
            listObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toHJsonString()

        val actual:List<Any> = this.sut.toData(root)

        val expected = listOf(1, true, "hello")

        assertEquals(expected, actual)
    }


    @Test
    fun toHJson_Set() {

        val root = setOf(1, true, "hello")

        val actual = this.sut.toHJson(root, root)

        val expected = hjson("expected") {
            setObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toHJsonString()

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_Set() {

        val root = hjson("expected") {
            setObject {
                primitiveObject("kotlin.Int", 1)
                boolean(true)
                string("hello")
            }
        }.toHJsonString()

        val actual:Set<Any> = this.sut.toData(root)

        val expected = setOf(1, true, "hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_Map() {

        val root = mapOf("a" to 1, "b" to true, "c" to "hello")

        val actual = this.sut.toHJson(root, root)

        val expected = hjson("expected") {
            mapObject {
                entry({ string("a") }) { primitiveObject("kotlin.Int", 1) }
                entry({ string("b") }) { boolean(true) }
                entry({ string("c") }) { string("hello") }
            }
        }.toHJsonString()

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_Map() {

        val root = hjson("expected") {
            mapObject {
                entry({ string("a") }) { primitiveObject("kotlin.Int", 1) }
                entry("b", true)
                entry("c", "hello")
            }
        }.toHJsonString()

        val actual:Map<String,Any> = this.sut.toData(root)

        val expected = mapOf("a" to 1, "b" to true, "c" to "hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_A() {

        val root = TestClassAAA("1: hello")
        root.setProp2(5)
        val dtA = sut.registry.findDatatypeByName("TestClassAAA")!!

        val actual = this.sut.toHJson(root, root)

        val expected = hjson("expected") {
            objectReferenceable(dtA.qualifiedName) {
                property("prop1", "1: hello")
                property("comp", null)
                property("refr", null)
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toHJsonString()

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_A() {

        val dtA = sut.registry.findDatatypeByName("TestClassAAA")!!

        val json = hjson("expected") {
            objectReferenceable(dtA.qualifiedName) {
                property("prop1", "hello")
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toHJsonString()

        val actual:TestClassAAA = this.sut.toData(json)

        val expected = TestClassAAA("hello")

        assertEquals(expected, actual)
    }

    @Test
    fun toHJson_A_2() {

        val root = TestClassAAA("1: hello")
        root.setProp2(5)
        root.comp = TestClassAAA("1.3")
        root.comp?.refr = root
        val dtA = sut.registry.findDatatypeByName("TestClassAAA")!!

        val actual = this.sut.toHJson(root, root)

        val expected = hjson("expected") {
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
        }.toHJsonString()

        assertEquals(expected, actual.toHJsonString())
    }

    @Test
    fun toData_A_2() {

        val dtA = sut.registry.findDatatypeByName("TestClassAAA")!!

        val json = hjson("expected") {
            objectReferenceable(dtA.qualifiedName) {
                property("prop1", "1: hello")
                property("comp") {
                    objectReferenceable(dtA.qualifiedName) {
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
        }.toHJsonString()

        val actual:TestClassAAA = this.sut.toData(json)

        val expected = TestClassAAA("1: hello")
        expected.setProp2(5)
        expected.comp = TestClassAAA("1.3")
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