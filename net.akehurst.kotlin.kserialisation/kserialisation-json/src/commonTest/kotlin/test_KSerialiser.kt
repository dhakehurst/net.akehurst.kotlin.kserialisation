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


import net.akehurst.kotlin.json.JsonBoolean
import net.akehurst.kotlin.json.JsonNumber
import net.akehurst.kotlin.json.JsonString
import net.akehurst.kotlin.json.json
import net.akehurst.kotlin.kserialisation.json.test.TestClassAAA
import net.akehurst.kotlin.kserialisation.json.test.TestEnumEEE
import net.akehurst.kotlinx.reflect.EnumValuesFunction
import net.akehurst.kotlinx.reflect.KotlinxReflect
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.clazz
import net.akehurst.language.types.builder.typesDomain
import net.akehurst.kotlinx.komposite.common.LookupMapper
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class test_KSerialiser {

    private val sut = KSerialiserJson()

    @BeforeTest
    fun setup() {
        this.sut.configureFromTypeModel(typesDomain("", true) {
            namespace("net.akehurst.kotlin.kserialisation.json.test") {
                primitive("Instant")
                enum("TestEnumEEE", emptyList())
                data("TestClassAAA") {
                    constructor_ {
                        parameter(setOf(),"prop1", "String")
                        parameter(setOf(),"comp", "TestClassAAA", true)
                    }
                    propertyOf(setOf(CMP, VAL), "prop1", "String")
                    propertyOf(setOf(CMP, VAR), "comp", "TestClassAAA")
                    propertyOf(setOf(REF, VAR), "refr", "TestClassAAA")
                    propertyOf(setOf(CMP, VAR), "prop2", "Integer")
                }
            }
        })
        /*
        this.sut.configureFromKompositeString(
            """
            namespace net.akehurst.kotlin.kserialisation.json.test
                class TestClassAAA {
                    cmp prop1
                    cmp comp
                }
        """.trimIndent()
        )
*/

        this.sut.registerKotlinStdPrimitives()
        this.sut.registerPrimitiveAsObject(
            Instant::class, //
            { value -> JsonNumber(value.toEpochMilliseconds().toString()) }, //
            { json -> Instant.fromEpochMilliseconds(json.asNumber().toLong()) }
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

        val root: Any = 1.toByte()
        println("class.simpleName: ${root::class.simpleName}")
        assertEquals("Byte", root::class.simpleName)

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("Byte"))!!
        assertEquals("Byte", dt.name.value)

        val expected = json("expected") {
            primitiveObject(dt.qualifiedName.value, root)
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Byte_1() {

        val dt = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("Byte"))!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedName.value, 1)
        }.toFormattedJsonString("  ", "  ")

        val actual: Byte = this.sut.toData(root)

        val expected = 1.toByte()

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Int_1() {

        val root = 1.toInt()

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("Int"))!!
        val expected = json("expected") {
            primitiveObject(dt.qualifiedName.value, root)
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Int_1() {

        val dt = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("Int"))!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedName.value, 1)
        }.toFormattedJsonString("  ", "  ")

        val actual: Int = this.sut.toData(root)

        val expected = 1.toInt()

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_Long_1() {

        val root = 1.toLong()

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("Long"))!!
        val expected = json("expected") {
            primitiveObject(dt.qualifiedName.value, root)
        }.toFormattedJsonString("  ", "  ")


        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Long_1() {

        val dt = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("Long"))!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedName.value, 1)
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
    fun toJson_Instant() {

        val now = Clock.System.now()
        val root = now

        val actual = this.sut.toJson(root, root)

        val dt = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("Instant"))!!

        val expected = json("expected") {
            primitiveObject(dt.qualifiedName.value, now.toEpochMilliseconds())
        }.toFormattedJsonString("  ", "  ")

        assertEquals(expected, actual.toFormattedJsonString("  ", "  "))
    }

    @Test
    fun toData_Instant() {

        val now = Clock.System.now()
        val dt = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("Instant"))!!
        val root = json("expected") {
            primitiveObject(dt.qualifiedName.value, now.toEpochMilliseconds())
        }.toFormattedJsonString("  ", "  ")

        val actual: Instant = this.sut.toData(root)

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

        val root = arrayOf<Any>(1, true, "hello")

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

        val expected = arrayOf<Any>(1, true, "hello")

        assertTrue(expected contentEquals actual)
    }


    @Test
    fun toJson_Unit_is_special_value_Nothing() {
        // kotlin.Unit is the agl value nothing (ObjectGraphAccessorMutator.nothing()), of the special type std.Nothing
        val root = listOf(1, Unit, "hello")

        val str = this.sut.toJson(root, root).toFormattedJsonString("  ", "  ")

        assertTrue(str.contains("\$SPECIAL") && str.contains("std.Nothing"), str)
        val actual: List<Any> = this.sut.toData(str)
        assertEquals(root, actual)
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

        val root = TestClassAAA("1: hello", null)
        root.prop2 = (5)
        val dtA = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("TestClassAAA"))!!

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            objectReferenceable(dtA.qualifiedName.value) {
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

        val dtA = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("TestClassAAA"))!!

        val json = json("expected") {
            objectReferenceable(dtA.qualifiedName.value) {
                property("prop1", "hello")
                property("prop2") {
                    primitiveObject("kotlin.Int", 5)
                }
            }
        }.toFormattedJsonString("  ", "  ")

        val actual: TestClassAAA = this.sut.toData(json)

        val expected = TestClassAAA("hello", null)

        assertEquals(expected, actual)
    }

    @Test
    fun toJson_A_2() {

        val root = TestClassAAA("1: hello", TestClassAAA("1.3", null))
        root.prop2 = (5)
        root.comp?.refr = root
        val dtA = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("TestClassAAA"))!!

        val actual = this.sut.toJson(root, root)

        val expected = json("expected") {
            objectReferenceable(dtA.qualifiedName.value) {
                property("prop1", "1: hello")
                property("comp") {
                    objectReferenceable(dtA.qualifiedName.value) {
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
        val dtA = sut.registry.findFirstDefinitionByNameOrNull(SimpleName("TestClassAAA"))!!
        println("dta = $dtA")
        println(KotlinxReflect.registeredClasses)
        println("dta = ${dtA.clazz}")

        val json = json("expected") {
            objectReferenceable(dtA.qualifiedName.value) {
                property("prop1", "1: hello")
                property("comp") {
                    objectReferenceable(dtA.qualifiedName.value) {
                        property("prop1", "1.3")
                        property("comp", null)
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

        val expected = TestClassAAA("1: hello", TestClassAAA("1.3", null))
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

    @Test
    fun toJson_same_object_twice_is_written_once() {
        // an object met more than once (by identity) is written in full once, and as a reference elsewhere
        val shared = TestClassAAA("shared", null)
        val root = listOf(shared, shared)

        val str = this.sut.toJson(root, root).toFormattedJsonString("  ", "  ")

        assertEquals(1, Regex("shared").findAll(str).count(), str)
        val actual: List<TestClassAAA> = this.sut.toData(str)
        assertEquals(2, actual.size)
        assertTrue(actual[0] === actual[1], "expected the same instance")
    }

    @Test
    fun toJson_equal_but_distinct_objects_are_both_written() {
        // objects are compared by identity, not by equals
        val root = listOf(TestClassAAA("same", null), TestClassAAA("same", null))

        val str = this.sut.toJson(root, root).toFormattedJsonString("  ", "  ")

        assertEquals(2, Regex("same").findAll(str).count(), str)
        val actual: List<TestClassAAA> = this.sut.toData(str)
        assertEquals(2, actual.size)
        assertTrue(actual[0] !== actual[1], "expected distinct instances")
    }

    @Test
    fun toJson_composite_cycle_through_constructor_gives_error() {
        // x.comp = y, y.comp = x : neither can be constructed first
        val x = TestClassAAA("x", null)
        val y = TestClassAAA("y", x)
        x.comp = y

        val str = this.sut.toJson(x, x).toFormattedJsonString("  ", "  ")

        val e = assertFailsWith<KSerialiserJsonException> { this.sut.toData<TestClassAAA>(str) }
        assertTrue(e.message!!.contains("its constructor arguments refer back to it"), e.message)
    }

    @Test
    fun toJson_registered_lookup_writes_key_and_reads_existing_object() {
        // an object a registered lookup gives a key for is written by that key, and found again by it
        val known = TestClassAAA("known-value", null)
        this.sut.registry.registerLookup(LookupMapper(
            name = "known",
            toKey = { v -> if (v === known) "the-known-one" else null },
            fromKey = { k -> if (k == "the-known-one") known else null }
        ))
        val root = listOf(known, TestClassAAA("other", null))

        val str = this.sut.toJson(root, root).toFormattedJsonString("  ", "  ")

        assertTrue(str.contains("\$LOOKUP") && str.contains("the-known-one") && !str.contains("known-value"), str)
        val actual: List<TestClassAAA> = this.sut.toData(str)
        assertTrue(actual[0] === known, "expected the registered instance")
        assertEquals("other", actual[1].prop1)
    }
}
