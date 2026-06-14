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

package net.akehurst.kotlin.json

import kotlin.test.*


class test_JsonProcessor {

    fun assertEquals(expected: JsonDocument, actual: JsonDocument) {
        assertEquals(expected.identity, actual.identity)
        assertEquals(expected.root, actual.root)
    }

    fun assertEquals(expected: JsonValue, actual: JsonValue) {
        when {
            expected is JsonNull && actual is JsonNull -> assertEquals(expected, actual)
            expected is JsonString && actual is JsonString -> assertEquals(expected, actual)
            expected is JsonNumber && actual is JsonNumber -> assertEquals(expected, actual)
            expected is JsonBoolean && actual is JsonBoolean -> assertEquals(expected, actual)
            expected is JsonArray && actual is JsonArray -> assertEquals(expected, actual)
            expected is JsonReference && actual is JsonReference -> assertEquals(expected, actual)
            expected is JsonReferencableObject && actual is JsonReferencableObject -> assertEquals(expected, actual)
            expected is JsonUnreferencableObject && actual is JsonUnreferencableObject -> assertEquals(expected, actual)
        }
    }

    fun assertEquals(expected: JsonNull, actual: JsonNull) {
        assertTrue(true)
    }

    fun assertEquals(expected: JsonString, actual: JsonString) {
        assertEquals(expected.encodedValue,actual.encodedValue)
    }

    fun assertEquals(expected: JsonNumber, actual: JsonNumber) {
        assertEquals(expected.toStringJson(),actual.toStringJson())
    }

    fun assertEquals(expected: JsonBoolean, actual: JsonBoolean) {
        assertEquals(expected.value,actual.value)
    }

    fun assertEquals(expected: JsonArray, actual: JsonArray) {
        assertEquals(expected.elements.size, actual.elements.size)
        for (i in 0 until expected.elements.size) {
            val expEl = expected.elements[i]
            val actEl = expected.elements[i]
            assertEquals(expEl,actEl)
        }
        expected.elements.forEach { }
    }

    fun assertEquals(expected: JsonReference, actual: JsonReference) {
        assertEquals(expected.document.identity,actual.document.identity)
        assertEquals(expected.refPath,actual.refPath)
    }

    fun assertEquals(expected: JsonReferencableObject, actual: JsonReferencableObject) {
        assertEquals(expected.document.identity,actual.document.identity)
        assertEquals(expected.path,actual.path)
        assertEquals(expected.property.size,actual.property.size)
        for(k in expected.property.keys) {
            val expEl = expected.property[k]!!
            val actEl = actual.property[k]!!
            assertEquals(expEl,actEl)
        }
    }

    fun assertEquals(expected: JsonUnreferencableObject, actual: JsonUnreferencableObject) {
        assertEquals(expected.property.size,actual.property.size)
        for(k in expected.property.keys) {
            val expEl = expected.property[k]!!
            val actEl = actual.property[k]!!
            assertEquals(expEl,actEl)
        }
    }


    @Test
    fun empty() {

        val jsonString = ""

        assertFailsWith<JsonParserException> {
            Json.process(jsonString)
        }

    }

    @Test
    fun boolean_false() {

        val jsonString = "false"

        val actual = Json.process(jsonString)

        val expected = json("json") {
            boolean(false)
        }

        assertEquals(expected, actual)

    }

    @Test
    fun boolean_true() {

        val jsonString = "true"

        val actual = Json.process(jsonString)

        val expected = json("json") {
            boolean(true)
        }

        assertEquals(expected, actual)

    }

    @Test
    fun number_1() {

        val jsonString = "1"

        val actual = Json.process(jsonString)

        val expected = json("json") {
            number(1)
        }

        assertEquals(expected, actual)

    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun negative_loohbehind() {
        val regex = Regex("(?<=x)a") // a preceded by x
        assertFalse(regex.matchesAt("ba",1))
        assertTrue(regex.matchesAt("xa",1))
    }
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun negative_loohbehind1() {
        val regex = Regex("(?<!x)a") // a NOT preceded by x
        assertTrue(regex.matchesAt("ba",1))
        assertFalse(regex.matchesAt("xa",1))
    }
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun negative_loohbehind2() {
        val regex = Regex("(?<=\\\\)a") // a preceded by \
        assertFalse(regex.matchesAt("ba",1))
        assertTrue(regex.matchesAt("\\a",1))
    }
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun negative_loohbehind3() {
        val regex = Regex("(?<!\\\\)a") // a NOT preceded by \
        assertTrue(regex.matchesAt("ba",1))
        assertFalse(regex.matchesAt("\\a",1))
    }
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun negative_loohbehind4() {
        val regex = Regex("(?<!\\\\)\\\\(.)") // \<any> NOT preceded by \
        assertTrue(regex.matchesAt("\\\\",0))
        assertFalse(regex.matchesAt("\\\\a",1))
    }
    @Test
    fun encode() {
        assertEquals("a",JsonString.encode("a"))
        assertEquals("\\\\a",JsonString.encode("\\a"))
        assertEquals("\\\\",JsonString.encode("\\"))
        assertEquals("\\n",JsonString.encode("\n"))
        assertEquals("\\\\n",JsonString.encode("\\n"))
        assertEquals("\\\"",JsonString.encode("\""))
    }
    @Test
    fun decode() {
        assertEquals("a",JsonString.decode("a"))
        assertEquals("\\a",JsonString.decode("\\\\a"))
        assertEquals("\\",JsonString.decode("\\\\"))
        assertEquals("\n",JsonString.decode("\\n"))
        assertEquals("\\n",JsonString.decode("\\\\n"))
        assertEquals("\"",JsonString.decode("\\\""))
    }

    @Test
    fun string() {

        val jsonString = "\"hello\""

        val actual = Json.process(jsonString)

        val expected = json("json") {
            string("hello")
        }

        assertEquals(expected, actual)

    }

    @Test
    fun string2() {

        val jsonString = "\"hello\\nWorld\""

        val actual = Json.process(jsonString)

        val expected = json("json") {
            string("hello\nWorld")
        }

        assertEquals(expected, actual)

    }

    @Test
    fun string3() {

        val jsonString = "\"hello\\\\nWorld\""

        val actual = Json.process(jsonString)

        val expected = json("json") {
            string("hello\\nWorld")
        }

        assertEquals(expected, actual)

    }


    @Test
    fun emptyArray() {

        val jsonString = "[]"

        val actual = Json.process(jsonString)

        val expected = json("json") {
            arrayJson {}
        }

        assertEquals(expected, actual)

    }

    @Test
    fun array() {

        val jsonString = "[ 1, true, \"hello\", {} ]"

        val actual = Json.process(jsonString)

        val expected = json("json") {
            arrayJson {
                number(1)
                boolean(true)
                string("hello")
                objectJson { }
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun emptyObject() {

        val jsonString = "{}"

        val actual = Json.process(jsonString);

        val expected = json("json") {
            objectJson { }
        }

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

        val expected = json("json") {
            objectJson {
                property("bProp", true)
                property("nProp", 1)
                property("sProp", "hello")
                property("aProp") {
                    arrayJson {
                        number(1)
                        boolean(true)
                        string("hello")
                        objectJson {  }
                    }
                }
                property("oProp") {
                    objectJson {
                        property("bProp", false)
                        property("nProp", 3.14)
                    }
                }
            }
        }

        assertEquals(expected, actual)

    }

}