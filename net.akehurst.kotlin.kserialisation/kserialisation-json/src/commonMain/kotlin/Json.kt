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

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import kotlin.js.JsName

object Json {

    private var _processor:LanguageProcessor? = null

    val REF = "${'$'}ref"

    internal fun processor(): LanguageProcessor {
        if (null== _processor) {
            val grammarStr = fetchGrammarStr()
            _processor = Agl.processor(
                    grammarStr,
                    SyntaxAnalyser(),
                    Formatter()
            )
        }
        return _processor!!
    }

    internal fun fetchGrammarStr(): String {
        return """
            namespace net.akehurst.kotlin.kserialisation.json
            
            grammar Json {
                skip WHITE_SPACE = "\s+" ;

                json = value ;
                value = literalValue | object | array ;
                object = '{' [ property / ',']* '}' ;
                property = DOUBLE_QUOTE_STRING ':' value ;
                array = '[' [ value / ',' ]* ']' ;
                literalValue = BOOLEAN < DOUBLE_QUOTE_STRING < NUMBER < NULL ;
                BOOLEAN             = 'true' | 'false' ;
                NUMBER              = "-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?" ;
                DOUBLE_QUOTE_STRING = "\"(?:\\?.)*?\"" ;
                NULL                = 'null' ;
            
            }
            
            """.trimIndent()


    }


    @JsName("process")
    fun process(jsonString:String) : JsonValue {
        return this.processor().process("json", jsonString)
    }
}