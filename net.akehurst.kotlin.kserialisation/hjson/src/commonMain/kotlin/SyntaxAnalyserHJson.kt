/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.kotlinx.collections.mutableStackOf
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.treedata.matchedTextNoSkip

class SyntaxAnalyserHJson(

) : SyntaxAnalyserByMethodRegistrationAbstract<HJsonDocument>() {

    private var _doc = HJsonDocument("hjson")
    private val _path = mutableStackOf<String>()

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<HJsonDocument>> = emptyMap()

    override fun registerHandlers() {
        super.register(this::hjson)
        super.registerFor("value", this::value_)
        super.registerFor("object", this::object_)
        super.register(this::property)
        super.registerForBeginHandler("property",this::property_begin)
        super.register(this::array)
        super.register(this::arrayElements)
        super.register(this::arrayElementsSeparated)
        super.register(this::arrayElementsSimple)
        super.register(this::name)
        super.register(this::literal)
        super.register(this::string)
        super.register(this::ID)
        super.register(this::NULL)
        super.register(this::BOOLEAN)
        super.register(this::NUMBER)
        super.register(this::QUOTELESS_STRING)
        super.register(this::DOUBLE_QUOTE_STRING)
        super.register(this::MULTI_LINE_STRING)
    }

    override fun <T : Any> clear(done: Set<SyntaxAnalyser<T>>) {
        super.clear(done)
        this._doc = HJsonDocument("hjson")
        this._path.clear()
    }

    // hjson = value ;
    fun hjson(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonDocument {
        this._doc.root = children[0] as HJsonValue
        return this._doc
    }

    // value = literal | object | array ;
    fun value_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonValue =
        children[0] as HJsonValue

    // object = '{' property* '}' ;
    fun object_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonValue {
        val props = (children[1] as List<Pair<String, HJsonValue>?>?)?.filterNotNull() ?: emptyList()
        return when {
            //HJsonReference
            1==props.size && props[0].first==HJson.REF && (props[0].second is HJsonString)-> {
                val refStr = (props[0].second as HJsonString).value
                HJsonReference(this._doc, refStr)
            }

            (props.any{it.first==HJsonDocument.KIND && it.second is HJsonString && props[0].second == HJsonDocument.ComplexObjectKind.OBJECT.asHJsonString }) ->{
                val obj = HJsonReferencableObject(this._doc, this._path.elements.toMutableList())
                props.forEach { (n, v) -> obj.setProperty(n, v) }
                obj
            }
            else -> {
                val obj = HJsonUnreferencableObject()
                props.forEach { (n, v) -> obj.setProperty(n, v) }
                 obj
            }
        }
    }

    // property = name ':' value ;
    fun property_begin(nodeInfo: SpptDataNodeInfo, siblings: List<Any?>, sentence: Sentence) {
        val name = sentence.matchedTextNoSkip(nodeInfo.node).substringBefore(":").trim()
        this._path.push(name)
    }

    fun property(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<String, HJsonValue> {
        this._path.pop()
        val name = children[0] as String
        val value = children[2] as HJsonValue
        return Pair(name, value)
    }

    // array = '[' arrayElements ']' ;
    fun array(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonArray {
        val l = children[1] as List<HJsonValue>
        return HJsonArray(l)
    }

    // arrayElements = arrayElementsSeparated | arrayElementsSimple ;
    fun arrayElements(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<HJsonValue> =
        children[0] as List<HJsonValue>

    // arrayElementsSeparated = [ value / ',' ]* ;
    fun arrayElementsSeparated(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<HJsonValue> =
        (children as List<Any>).toSeparatedList<Any, HJsonValue, String>().items

    // arrayElementsSimple =  value* ;
    fun arrayElementsSimple(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<HJsonValue> =
        children as List<HJsonValue>

    // name = DOUBLE_QUOTE_STRING | ID ;
    fun name(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        val v = children[0]
        return when (v) {
            is HJsonString -> v.value
            is String -> v
            else -> error("should never happen")
        }
    }

    // literal = string | NUMBER | BOOLEAN | NULL ;
    fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonValue =
        children[0] as HJsonValue

    // string = DOUBLE_QUOTE_STRING | QUOTELESS_STRING | MULTI_LINE_STRING ;
    fun string(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonString =
        children[0] as HJsonString

    // leaf ID = "[^\{\}\[\],:\n\r\t ]+" ;
    fun ID(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        sentence.matchedTextNoSkip(nodeInfo.node)

    // leaf NULL = 'null' ;
    fun NULL(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonNull =
        HJsonNull

    // leaf BOOLEAN = "true|false" ;
    fun BOOLEAN(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonBoolean {
        val str = sentence.matchedTextNoSkip(nodeInfo.node)
        return when (str) {
            "true" -> HJsonBoolean(true)
            "false" -> HJsonBoolean(false)
            else -> error("Invalid value for BOOLEAN '$str'")
        }
    }

    // leaf NUMBER = "-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?" ;
    fun NUMBER(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonNumber {
        val str = sentence.matchedTextNoSkip(nodeInfo.node)
        return HJsonNumber(str)
    }

    // leaf QUOTELESS_STRING = "[^\{\}\[\],:\n][^\n]*[\n]" ;
    fun QUOTELESS_STRING(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonString {
        val str = sentence.matchedTextNoSkip(nodeInfo.node).dropLast(1)
        return HJsonString(str)
    }

    // leaf DOUBLE_QUOTE_STRING = "\"([^"\\]|\\.)*\"" ;
    fun DOUBLE_QUOTE_STRING(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonString {
        val str = sentence.matchedTextNoSkip(nodeInfo.node)
        val s = str.substring(1, str.length - 1)
        return HJsonString(s)
    }

    // leaf MULTI_LINE_STRING = "'''([^"\\]|\\.)*'''" ;
    fun MULTI_LINE_STRING(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): HJsonString {
        val str = sentence.matchedTextNoSkip(nodeInfo.node)
        val s = str.substring(3, str.length - 3)
        return HJsonString(s)
    }
}