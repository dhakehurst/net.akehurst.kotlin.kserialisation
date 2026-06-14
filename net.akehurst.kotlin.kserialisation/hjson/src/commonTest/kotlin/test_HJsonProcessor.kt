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

package net.akehurst.hjson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class test_HJsonProcessor {

    @Test
    fun empty() {

        val jsonString = ""

        assertFailsWith<HJsonParserException> {
            val actual = HJson.process(jsonString)
        }

    }

    @Test
    fun boolean_false() {

        val jsonString = "false"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive(false)
        }

        assertEquals(expected, actual)

    }

    @Test
    fun boolean_true() {

        val jsonString = "true"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive(true)
        }

        assertEquals(expected, actual)

    }

    @Test
    fun string_00() {

        val jsonString = "00"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            string("00")
        }

        assertEquals(expected, actual)

    }

    @Test
    fun number_1() {

        val jsonString = "1"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive(1)
        }

        assertEquals(expected, actual)

    }

    @Test
    fun number_3p141() {

        val jsonString = "3.141"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive(3.141)
        }

        assertEquals(expected, actual)

    }


    @Test
    fun string() {

        val jsonString = "\"hello\""

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive("hello")
        }

        assertEquals(expected, actual)

    }

    @Test
    fun multiline_string() {

        val jsonString = """
            '''hello
            world'''
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive("""
                hello
                world
            """.trimIndent())
        }

        assertEquals(expected, actual)

    }

    @Test
    fun emptyArray() {

        val jsonString = "[]"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayJson {}
        }

        assertEquals(expected, actual)

    }

    @Test
    fun array_x1_int() {

        val jsonString = "[ 1 ]"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayJson {
                number(1)
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun array_x1_boolean() {

        val jsonString = "[ true ]"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayJson {
                boolean(true)
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun array_x1_null() {

        val jsonString = "[ null ]"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayJson {
                nullValue()
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun array_x1_dblQ_string() {

        val jsonString = "[ \"Hello\" ]"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayJson {
                string("Hello")
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun array_x1_noQ_string() {

        val jsonString = """
            [ Hello
            ]
        """


        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayJson {
                string("Hello")
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun array_x4() {

        val jsonString = "[ 1, true, \"hello\", {} ]"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayJson {
                primitive(1)
                primitive(true)
                primitive("hello")
                objectJson { }
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun emptyObject() {

        val jsonString = "{}"

        val actual = HJson.process(jsonString);

        val expected = hjson("json") {
            objectJson { }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun emptyObject_with_comments() {

        val jsonString = """
        {
            #hash comment
            # and another
            
            // single line comment
            // and a second one
            
            /* multi line 
               comment also
            */
        }
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            objectJson { }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun object_json() {

        val jsonString = """
            {
                "bProp" : true,
                "nProp" : 1,
                "sProp" : "hello",
                "aProp" : [ 1, true, "hello", {} ],
                "oProp" : {
                    "bProp": false,
                    "nProp" : 3.14
                }
            }
        """.trimIndent()

        val actual = HJson.process(jsonString);

        val expected = hjson("hjson") {
            objectJson {
                property("bProp", true)
                property("nProp", 1)
                property("sProp", "hello")
                property("aProp") {
                    arrayJson {
                        primitive(1)
                        primitive(true)
                        primitive("hello")
                        objectJson { }
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
    @Test
    fun object_hjson() {

        val jsonString = """
        {
          bProp : true
          nProp : 1
          sProp : hello
          aProp : [
            1
            true
            hello
            {}
          ]
          oProp : {
            bProp : false
            nProp : 3.14
          }
        }
        """.trimIndent()

        val actual = HJson.process(jsonString);

        val expected = hjson("json") {
            objectJson {
                property("bProp", true)
                property("nProp", 1)
                property("sProp", "hello")
                property("aProp") {
                    arrayJson {
                        primitive(1)
                        primitive(true)
                        primitive("hello")
                        objectJson { }
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

    @Test
    fun object_with_string() {

        val jsonString = """
        {
          prop : Hello World
        }
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            objectJson {
                property("prop", "Hello World")
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun arrayObject() {
        val D = "$"

        val jsonString = """
            {
              ${D}kind : ${D}ARRAY
              ${D}elements : [
                1
                true
                {}
                hello
              ]
            }
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayObject {
                primitive(1)
                primitive(true)
                objectJson { }
                primitive("hello")
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun listObject() {
        val D = "$"

        val jsonString = """
            {
              ${D}kind : ${D}LIST
              ${D}elements : [
                1
                true
                {}
                hello
              ]
            }
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            listObject {
                primitive(1)
                primitive(true)
                objectJson { }
                primitive("hello")
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun reference() {
        val D = "$"

        val jsonString = """
            {
              ${D}kind : ${D}OBJECT
              ${D}class : A
              prim: 1
              obj1: {
                ${D}kind : ${D}OBJECT
                ${D}class : B
                refr: { ${D}ref: "#/" }
              }
            }
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            objectReferenceable("A") {
                property("prim", 1)
                property("obj1") {
                    objectReferenceable("B") {
                        property("refr") {
                            reference("#/")
                        }
                    }
                }
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun bug1_pass() {
        val D = "$"

        val jsonString = """
{
  artefactDefinition:
  {
    elements: [
      {
        ${D}kind : ${D}OBJECT
        ${D}class: de.itemis.vistraq.traceability.computational.traceabilityInformationModel.definition.ArtefactDefinition
        owner: {
          type: Reference
          ref: "#/"
        }
      }
    ]
  }
}
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
           objectJson {
               property("artefactDefinition") {
                   objectJson {
                       property("elements") {
                           arrayJson {
                               objectReferenceable("de.itemis.vistraq.traceability.computational.traceabilityInformationModel.definition.ArtefactDefinition") {
                                   property("owner") {
                                       reference("#/")
                                   }
                               }
                           }
                       }
                   }
               }
           }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun bug1_fail() {
        val D = "$"

        val jsonString = """
{
  artefactDefinition:
  {
    elements:
    [
      {
        class: de.itemis.vistraq.traceability.computational.traceabilityInformationModel.definition.ArtefactDefinition
        owner:
        {
          type: Reference
          ref: "#/"
        }
      }
    ]
  }
}
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            listObject {
                primitive(1)
                primitive(true)
                objectJson { }
                primitive("hello")
            }
        }

        assertEquals(expected, actual)

    }

}