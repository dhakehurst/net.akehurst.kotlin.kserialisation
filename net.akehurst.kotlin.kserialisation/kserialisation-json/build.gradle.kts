plugins {
    id("net.akehurst.kotlin.kt2ts") version "1.0.0"
}

val version_komposite:String by project
//val version_agl:String by project

val group_klock:String by project
val version_klock:String by project

val version_kotlinx:String by project
val version_json = "1.1.0"

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

val tsdDir ="${buildDir}/tmp/jsJar/ts"

kotlin {
    sourceSets {
        val jsMain by getting {
            resources.srcDir("${tsdDir}")
        }
    }
}

kt2ts {
    localJvmName.set("jvm8")
    modulesConfigurationName.set("jvm8RuntimeClasspath")
    outputDirectory.set(file("${tsdDir}"))
    declarationsFile.set(file("${tsdDir}/${project.group}-${project.name}.d.ts"))
    classPatterns.set(listOf(
            "net.akehurst.kotlin.kserialisation.json.*"
    ))
}
tasks.getByName("generateTypescriptDefinitionFile").dependsOn("jvm8MainClasses")
tasks.getByName("jsJar").dependsOn("generateTypescriptDefinitionFile")