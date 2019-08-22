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

import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.DatatypeModel
import net.akehurst.kotlin.komposite.api.DatatypeProperty
import net.akehurst.kotlin.komposite.api.Namespace
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt2ast.UnableToTransformSppt2AstExeception
import net.akehurst.language.processor.BranchHandler
import net.akehurst.language.processor.SyntaxAnalyserAbstract

class SyntaxAnalyser : SyntaxAnalyserAbstract() {

    class SyntaxAnalyserException : RuntimeException {
        constructor(message: String) : super(message)
    }

    init {
        // could autodetect these by reflection, for jvm, but faster if registered
        this.register("json", this::json as BranchHandler<JsonValue>)
        this.register("value", this::value as BranchHandler<JsonValue>)
        this.register("object", this::object_ as BranchHandler<JsonValue>)
        this.register("property", this::property as BranchHandler<Pair<String,JsonValue>>)
        this.register("array", this::array as BranchHandler<JsonArray>)
        this.register("literalValue", this::literalValue as BranchHandler<JsonValue>)
        this.register("BOOLEAN", this::BOOLEAN as BranchHandler<JsonBoolean>)
        this.register("NUMBER", this::NUMBER as BranchHandler<JsonNumber>)
        this.register("DOUBLE_QUOTE_STRING", this::DOUBLE_QUOTE_STRING as BranchHandler<DatatypeProperty>)
        this.register("NULL", this::NULL as BranchHandler<JsonNull>)
    }

    override fun clear() {

    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        val doc = JsonDocument("json")
        doc.root = this.transform(sppt.root.asBranch, doc)
        return doc as T
    }

    // json = value ;
    fun json(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): JsonValue {
        return super.transform(children[0],arg)
    }

    // value = literalValue | object | array ;
    fun value(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): JsonValue {
        return super.transform(children[0],arg)
    }

    // object = '{' property* '}' ;
    fun object_(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): JsonValue {
        val properties = children[0].branchNonSkipChildren.associate {
            super.transform<Pair<String,JsonValue>>(it,arg)
        }

        return if (1==properties.size && properties.containsKey(Json.REF)) {
            val refPath = properties[Json.REF]!!.asString().value.split("/")
            JsonReference(arg as JsonDocument, refPath)
        } else {
            val path = emptyList<String>()
            TODO("path needs a real value, but this class is no longer used, do we still need it?")
            val obj = JsonReferencableObject(arg as JsonDocument, path)
            obj.property = properties
            obj
        }
    }

    // property = DOUBLE_QUOTE_STRING ':' value ;
    fun property(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Pair<String,JsonValue> {
        val nameWithQuotes = children[0].nonSkipMatchedText
        val name = nameWithQuotes.substring(1, nameWithQuotes.length-1)
        val value =  super.transform<JsonValue>(children[1],arg)
        return Pair(name, value)
    }

    // array = '[' [ value / ',' ]+ ']' ;
    fun array(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): JsonArray {
        val list = children[0].branchNonSkipChildren.map {
            super.transform<JsonValue>(it, arg)
        }
        val json = JsonArray()
        json.elements = list // Does this need to be a mutable list ?
        return json
    }

    // literalValue = BOOLEAN < DOUBLE_QUOTE_STRING < NUMBER < NULL ;
    fun literalValue(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): JsonValue {
        return super.transform(children[0],arg)
    }

    // BOOLEAN             = 'true' | 'false' ;
    fun BOOLEAN(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): JsonBoolean {
        val value = target.nonSkipMatchedText.toBoolean()
        return JsonBoolean(value)
    }

    // NUMBER              = "-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?" ;
    fun NUMBER(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): JsonNumber {
        val value = target.nonSkipMatchedText
        return JsonNumber(value)
    }

    // DOUBLE_QUOTE_STRING = "\"(?:\\?.)*?\"" ;
    fun DOUBLE_QUOTE_STRING(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): JsonString {
        val valueWithQuotes = target.nonSkipMatchedText
        val value = valueWithQuotes.substring(1, valueWithQuotes.length-1)
        return JsonString(value)
    }

    // NULL                = 'null' ;
    fun NULL(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): JsonNull {
        return JsonNull
    }

}