import net.akehurst.kotlin.kt2ts.plugin.gradle.GenerateDynamicRequire

plugins {
    id("net.akehurst.kotlin.kt2ts") version "1.2.0"
}

val version_komposite:String by project
//val version_agl:String by project

val group_klock:String by project
val version_klock:String by project

val version_kotlinx:String by project
val version_json:String by project

dependencies {

    commonMainApi("net.akehurst.kotlin.json:json:$version_json")

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")

    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlin.json:json:$version_json")


    commonTestImplementation ("${group_klock}:klock:${version_klock}")


    // because IntelliJ won't resolve Implementation dependencies at runtime!
    commonTestImplementation(kotlin("reflect"))
    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
}

kt2ts {
    jvmTargetName.set("jvm8")
    classPatterns.set(listOf(
            "net.akehurst.kotlin.kserialisation.json.*"
    ))
}

project.tasks.create<GenerateDynamicRequire>(GenerateDynamicRequire.NAME) {
    group = "generate"
    dependsOn("jsTestClasses")
    tgtDirectory.set(rootProject.layout.buildDirectory.dir("js/packages_imported/net.akehurst.kotlinx-kotlinx-reflect/1.2.0"))
    dynamicImport.set(listOf(
            "net.akehurst.kotlin.kserialisation:kserialisation-json-test"
    ))
}