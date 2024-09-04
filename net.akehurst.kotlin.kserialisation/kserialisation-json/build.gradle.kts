val version_klock:String by project

dependencies {

    commonMainApi(libs.nak.json)
    commonMainApi(libs.nak.komposite.processor)
    commonMainApi(libs.nak.komposite.common)

    commonMainImplementation(libs.nak.kotlinx.collections)
    commonMainImplementation(libs.nak.kotlinx.reflect)

    commonTestImplementation ("com.soywiz.korlibs.klock:klock:${version_klock}")
}

exportPublic {
    exportPatterns.set(listOf(
        "net.akehurst.kotlin.kserialisation.json.**",
    ))
}