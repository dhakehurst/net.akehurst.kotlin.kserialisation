plugins {
    id("project-conventions")
    alias(libs.plugins.kotlinx.reflect)
}
val version_klock:String by project

dependencies {

    commonMainApi(project(":json"))

    commonMainImplementation(libs.nal.agl.processor) //for base language stuff
    commonMainImplementation(libs.nal.kotlinx.komposite) //for komposite walker
    commonMainImplementation(libs.nak.kotlinx.collections)
    commonMainImplementation(libs.nak.kotlinx.reflect)

}

exportPublic {
    exportPatterns.set(listOf(
        "net.akehurst.kotlin.kserialisation.json.**",
    ))
}

kotlinxReflect {
    // the test classes; on JS, wasm and native reflection only knows the members of registered classes
    forReflectionTest.set(listOf("net.akehurst.kotlin.kserialisation.json.test.*"))
}
