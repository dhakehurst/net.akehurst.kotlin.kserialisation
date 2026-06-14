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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.simple.ContextWithScope

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder

object HJson {

    val REF = "\$ref"
    val KEY_WORDS = arrayOf("true", "false", "null")

    internal val processor: LanguageProcessor<HJsonDocument, ContextWithScope<Any,Any>> by lazy {
        val grammarStr = fetchGrammarStr()
        val res = Agl.processorFromString(
            grammarDefinitionStr = grammarStr,
            Agl.configuration<HJsonDocument, ContextWithScope<Any,Any>> {
                //typeModelResolver { p -> ProcessResultDefault(TypeModelSimple.create(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                syntaxAnalyserResolver { _ -> ProcessResultDefault(SyntaxAnalyserHJson()) }
                semanticAnalyserResolver { _ -> ProcessResultDefault(SemanticAnalyserHJson()) }
            }
        )
        val proc = when {
            res.issues.errors.isEmpty() -> res.processor!!
            else -> error(res.issues.toString())
        }
        proc.buildFor()
    }

    internal fun fetchGrammarStr(): String {
        return """
            namespace net.akehurst.hjson
            
            grammar HJson {
                skip leaf WHITE_SPACE = "\s+" ;
                skip leaf COMMENT
                 = "/\*[^*]*\*+([^*/][^*]*\*+)*/"
                 | "(//|#)[^\n\r]*"
                 ;
    
                hjson = value ;
                value = literal | object | array ;
                object = '{' property* '}' ;
                property = name ':' value ','?;
                array = '[' arrayElements ']' ;
                arrayElements = arrayElementsSeparated | arrayElementsSimple ;
                arrayElementsSeparated = [ value / ',' ]2+ ;
                arrayElementsSimple =  value* ;
                name = DOUBLE_QUOTE_STRING | ID ;
                
                literal = string | NUMBER | BOOLEAN | NULL ;
                string = QUOTELESS_STRING | DOUBLE_QUOTE_STRING | MULTI_LINE_STRING ;
                
                leaf ID = "[^\{\}\[\],:\n\r\t ]+" ;
                leaf NULL = 'null' ;
                leaf BOOLEAN = "true|false" ;
                leaf NUMBER = "-?(0|[0-9]+)([.][0-9]+)?([eE][+-]?[0-9]+)?" ;
                leaf QUOTELESS_STRING = "[^\{\}\[\],:\n][^\n]*([\n])" ;
                leaf DOUBLE_QUOTE_STRING = "\"([^\n\"\\]|\\.)*\"" ;
                leaf MULTI_LINE_STRING = "'''([^\\]|\\.)*?'''" ;
            }
            
            """.trimIndent()
    }

    fun process(jsonString: String): HJsonDocument {
        val res = this.processor.process(jsonString)
        return when {
            res.allIssues.errors.isEmpty() -> res.asm!!
            else -> throw HJsonParserException(res.allIssues.toString(), res.allIssues.errors.first().location!!.line, res.allIssues.errors.first().location!!.column, res.allIssues.errors.first().message)
        }
        //return HJsonParser.process(jsonString)
    }
}