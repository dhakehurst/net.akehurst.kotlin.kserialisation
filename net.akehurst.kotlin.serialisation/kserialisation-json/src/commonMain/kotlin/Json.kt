package net.akehurst.kotlin.kserialisation.json

import net.akehurst.kotlin.komposite.api.DatatypeModel
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import kotlin.js.JsName

object Json {

    private var _processor:LanguageProcessor? = null

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