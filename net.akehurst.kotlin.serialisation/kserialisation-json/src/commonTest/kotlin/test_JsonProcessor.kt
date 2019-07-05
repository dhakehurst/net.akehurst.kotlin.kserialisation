package net.akehurst.kotlin.kserialisation.json

import kotlin.test.Test
import kotlin.test.assertEquals


class test_JsonProcessor {

    @Test
    fun empty() {

        val jsonString = ""

        val actual = Json.process(jsonString)

        val expected = JsonObject(emptyMap())

        assertEquals(expected, actual)

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
                    "bProp": true,
                    "nProp" : 1
                }
            }
        """.trimIndent()

        val actual = Json.process(jsonString);

        val expected = JsonObject(emptyMap())

        assertEquals(expected, actual)

    }

}