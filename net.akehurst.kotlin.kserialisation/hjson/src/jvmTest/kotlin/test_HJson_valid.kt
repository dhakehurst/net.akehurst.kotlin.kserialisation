package net.akehurst.hjson

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class test_HJson_valid{

    companion object {

        var sourceFiles = this::class.java.getResourceAsStream("/valid/").use {
            if (it == null)
                emptyList()
            else
                BufferedReader(InputStreamReader(it)).readLines()
        }

        @JvmStatic
        fun data(): Iterable<Array<Any>> {
            val col = mutableListOf<Array<Any>>()
            for (sourceFile in sourceFiles) {
                val filePath = "/valid/$sourceFile"
                // val inps = ClassLoader.getSystemClassLoader().getResourceAsStream(sourceFile)
                val inps = this::class.java.getResourceAsStream(filePath)

                val br = BufferedReader(InputStreamReader(inps))
                var text = br.readText()
                col.add(arrayOf(Data(sourceFile, text)))
            }
            return col
        }
    }

    class Data(val sourceFile: String, val text: String) {
        // --- Object ---
        override fun toString(): String {
            return this.sourceFile
        }
    }

    @BeforeTest
    fun setup() {
    }

    @ParameterizedTest
    @MethodSource("data")
    fun test(data: Data) {
        if (data.sourceFile.startsWith("fail")) {
            assertFailsWith(HJsonParserException::class) {
                HJson.process(data.text)
            }
        } else {
            val doc = HJson.process(data.text)
            assertNotNull(doc)
        }
    }

}