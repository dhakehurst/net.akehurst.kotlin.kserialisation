plugins {
    id("project-conventions")
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