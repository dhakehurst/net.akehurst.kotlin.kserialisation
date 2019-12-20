import net.akehurst.kotlin.kt2ts.plugin.gradle.GenerateDynamicRequire

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