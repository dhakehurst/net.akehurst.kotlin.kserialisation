plugins {
    id("net.akehurst.kotlin.kt2ts") version "1.1.0"
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
    localJvmName.set("jvm8")
    classPatterns.set(listOf(
            "net.akehurst.kotlin.kserialisation.json.*"
    ))
}
