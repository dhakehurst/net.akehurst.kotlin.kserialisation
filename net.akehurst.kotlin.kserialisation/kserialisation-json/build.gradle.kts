plugins {
    //id("net.akehurst.kotlinx.kotlinx-reflect-gradle-plugin") version("1.4.1")
    //id("net.akehurst.kotlin.gradle.plugin.exportPublic") version("1.3.0")
}

val version_komposite:String by project
val version_klock:String by project
val version_kotlinx:String by project
val version_json:String by project

dependencies {

    commonMainApi("net.akehurst.kotlin.json:json:$version_json")

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")

    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlin.json:json:$version_json")


    commonTestImplementation ("com.soywiz.korlibs.klock:klock:${version_klock}")
}

//tests require reflection
//kotlinxReflect {
//    forReflection.set(listOf(
//        "net.akehurst.kotlin.kserialisation.json.TestClassAAA"
//    ))
//}

/*
project.tasks.create<GenerateDynamicRequire>(GenerateDynamicRequire.NAME) {
    group = "generate"
    dependsOn("jsLegacyTestClasses")
    tgtDirectory.set(rootProject.layout.buildDirectory.dir("js/packages_imported/net.akehurst.kotlinx-kotlinx-reflect-js-legacy/1.4.1"))
    dynamicImport.set(listOf(
            "net.akehurst.kotlin.kserialisation:kserialisation-json-test"
    ))
}
 */