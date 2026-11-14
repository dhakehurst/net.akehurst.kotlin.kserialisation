/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import kotlin.test.Test
import kotlin.test.assertEquals

class test_JsonReference {

    @Test
    fun stringToList_standard_form() {
        assertEquals(listOf("a", "b"), JsonReference.stringToList("#/a/b"))
    }

    @Test
    fun stringToList_legacy_form() {
        assertEquals(listOf("a", "b"), JsonReference.stringToList("/a/b"))
    }

    @Test
    fun stringToList_root() {
        assertEquals(emptyList<String>(), JsonReference.stringToList("#/"))
        assertEquals(emptyList<String>(), JsonReference.stringToList("#"))
        assertEquals(emptyList<String>(), JsonReference.stringToList("/"))
    }

    @Test
    fun reference_is_written_in_standard_form() {
        val ref = JsonReference(JsonDocument("test"), listOf("a", "b"))
        assertEquals("""{ "${'$'}ref" : "#/a/b" }""", ref.toStringJson())
    }
}
