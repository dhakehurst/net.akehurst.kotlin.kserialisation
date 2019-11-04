val version_hjson = "1.0.0"
val version_kotlinx:String by project
val version_komposite:String by project

val group_klock:String by project
val version_klock:String by project

val version_agl:String by project

dependencies {

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")

    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlin.hjson:hjson:$version_hjson")


    commonTestImplementation ("${group_klock}:klock:${version_klock}")


    // because IntelliJ won't resolve Implementation dependencies at runtime!
    commonTestImplementation(kotlin("reflect"))
    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
    commonTestImplementation("net.akehurst.language:agl-processor:${version_agl}")
}