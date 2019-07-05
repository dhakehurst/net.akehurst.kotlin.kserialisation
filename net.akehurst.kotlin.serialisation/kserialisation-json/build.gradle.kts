val version_komposite:String by project
val version_agl:String by project

dependencies {

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")
    commonMainImplementation("net.akehurst.language:agl-processor:${version_agl}")
}