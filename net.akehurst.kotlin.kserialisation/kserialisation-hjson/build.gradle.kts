val version_klock:String by project

dependencies {

    commonMainApi(libs.nak.hjson)
    commonMainApi(libs.nak.komposite.processor)

    commonMainImplementation(libs.nak.komposite.common)
    commonMainImplementation(libs.nak.kotlinx.collections)
    commonMainImplementation(libs.nak.kotlinx.reflect)

    commonTestImplementation ("com.soywiz.korlibs.klock:klock:${version_klock}")


    // because IntelliJ won't resolve Implementation dependencies at runtime!
    //commonTestImplementation(kotlin("reflect"))
    //commonTestImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
    //commonTestImplementation("net.akehurst.language:agl-processor:${version_agl}")

}

exportPublic {
    exportPatterns.set(listOf(
        "net.akehurst.kotlin.kserialisation.hjson.**",
    ))
}