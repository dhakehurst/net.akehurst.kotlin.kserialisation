val version_hjson:String by project
val version_kotlinx:String by project
val version_komposite:String by project

val version_klock:String by project
val version_agl:String by project

dependencies {

    commonMainApi("net.akehurst.kotlin.hjson:hjson:$version_hjson")

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")


    commonTestImplementation ("com.soywiz.korlibs.klock:klock:${version_klock}")


    // because IntelliJ won't resolve Implementation dependencies at runtime!
    commonTestImplementation(kotlin("reflect"))
    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
    commonTestImplementation("net.akehurst.language:agl-processor:${version_agl}")
}