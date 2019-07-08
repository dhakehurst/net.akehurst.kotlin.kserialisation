package net.akehurst.kotlin.kserialisation.json

import net.akehurst.language.api.parser.ParseFailedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.expect


class test_JsonProcessor {

    @Test
    fun empty() {

        val jsonString = ""

        assertFailsWith<ParseFailedException> {
            val actual = Json.process(jsonString)
        }

    }

    @Test
    fun boolean_false() {

        val jsonString = "false"

        val actual = Json.process(jsonString)

        val expected = JsonBoolean(false)

        assertEquals(expected, actual)

    }

    @Test
    fun boolean_true() {

        val jsonString = "true"

        val actual = Json.process(jsonString)

        val expected = JsonBoolean(true)

        assertEquals(expected, actual)

    }

    @Test
    fun number_1() {

        val jsonString = "1"

        val actual = Json.process(jsonString)

        val expected = JsonNumber("1")

        assertEquals(expected, actual)

    }

    @Test
    fun emptyArray() {

        val jsonString = "[]"

        val actual = Json.process(jsonString)

        val expected = JsonArray(emptyList())

        assertEquals(expected, actual)

    }

    @Test
    fun array() {

        val jsonString = "[ 1, true, \"hello\", {} ]"

        val actual = Json.process(jsonString)

        val expected = JsonArray(listOf(JsonNumber("1"), JsonBoolean(true), JsonString("hello"), JsonObject(emptyMap())))

        assertEquals(expected, actual)

    }

    @Test
    fun emptyObject() {

        val jsonString = "{}"

        val actual = Json.process(jsonString);

        val expected = JsonObject(emptyMap())

        assertEquals(expected, actual)

    }


    @Test
    fun object_() {

        val jsonString = """
            {
                "bProp": true,
                "nProp" : 1,
                "sProp" : "hello",
                "aProp" : [ 1, true, "hello", {} ],
                "oProp" : {
                    "bProp": false,
                    "nProp" : 3.14
                }
            }
        """.trimIndent()

        val actual = Json.process(jsonString);

        val expected = JsonObject(mapOf(
                "bProp" to JsonBoolean(true),
                "nProp" to JsonNumber("1"),
                "sProp" to JsonString("hello"),
                "aProp" to JsonArray(listOf(JsonNumber("1"), JsonBoolean(true), JsonString("hello"), JsonObject(emptyMap()))),
                "oProp" to JsonObject(mapOf(
                        "bProp" to JsonBoolean(false),
                        "nProp" to JsonNumber("3.14")
                ))
        ))

        assertEquals(expected, actual)

    }

}