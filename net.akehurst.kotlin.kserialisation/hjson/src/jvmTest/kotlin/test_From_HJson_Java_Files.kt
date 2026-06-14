package net.akehurst.hjson


import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.*

class test_From_HJson_Java_Files {

    companion object {

        var sourceFiles = this::class.java.getResourceAsStream("/from_hjson-java/").use {
            if (it == null)
                emptyList()
            else
                BufferedReader(InputStreamReader(it)).readLines()
        }

        @JvmStatic
        fun data(): Iterable<Array<Any>> {
            val col = mutableListOf<Array<Any>>()
            for (sourceFile in sourceFiles) {
                val filePath = "/from_hjson-java/$sourceFile"
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
    fun parse(data: Data) {
        if (data.sourceFile.startsWith("fail")) {
            val res = HJson.processor.parse(data.text)
            assertTrue(res.issues.errors.isNotEmpty())
        } else {
            val res = HJson.processor.parse(data.text)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    fun process(data: Data) {
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