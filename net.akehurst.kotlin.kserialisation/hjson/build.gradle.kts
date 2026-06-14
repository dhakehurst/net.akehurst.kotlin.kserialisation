plugins {
    id("project-conventions")
}
dependencies {
    //for class Stack used in HJsonParser
    commonMainImplementation(libs.nak.kotlinx.collections)
    commonMainImplementation(libs.nal.agl.processor)

    //commonMainImplementation("net.akehurst.kotlin.json:json:1.2.1")
}
