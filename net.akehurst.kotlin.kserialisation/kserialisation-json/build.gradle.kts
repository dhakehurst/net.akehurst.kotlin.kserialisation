val version_komposite:String by project
val version_klock:String by project
val version_kotlinx:String by project
val version_json:String by project

dependencies {

    commonMainApi("net.akehurst.kotlin.json:json:$version_json")
    commonMainApi("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")

    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlin.json:json:$version_json")


    commonTestImplementation ("com.soywiz.korlibs.klock:klock:${version_klock}")
}

exportPublic {
    exportPatterns.set(listOf(
        "net.akehurst.kotlin.kserialisation.json.**",
    ))
}