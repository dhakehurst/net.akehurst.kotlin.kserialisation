[![Maven Central](https://img.shields.io/maven-central/v/net.akehurst.kotlin.kserialisation/kserialisation-json.svg)](https://search.maven.org/artifact/net.akehurst.kotlin.kserialisation/kserialisation-json)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
![Kotlin Version](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fraw.githubusercontent.com%2Fdhakehurst%2Fnet.akehurst.kotlin.kserialisation%2Frefs%2Fheads%2Fmaster%2Fnet.akehurst.kotlin.kserialisation%2Fgradle%2Flibs.versions.toml&search=kotlin%20%3D%20%22(%5B%5E%22%5D%2B)%22&replace=%241&label=Kotlin&color=green&link=https%3A%2F%2Fkotlinlang.org%2F)


# net.akehurst.kotlin.kserialisation

A Kotlin Multiplatform library for serialising Kotlin object graphs to JSON or [HJSON](https://hjson.github.io/) and back again,
including graphs that contain shared objects and references (cycles).

Unlike annotation or compiler-plugin based serialisers, kserialisation is driven by a **type model** that describes your classes,
which properties are *composite* (owned, written in full) and which are *references* (written as a pointer to an object that is
written elsewhere in the same document). The object graph is walked using the
komposite walker (`net.akehurst.language:kotlinx-komposite`), and objects are re-created on read using
`net.akehurst.kotlinx:kotlinx-reflect`.

Features:

* JSON and HJSON formats, with the same model and API
* composite vs. reference properties, so object graphs with back-references and cycles round-trip
* an object that is met more than once (by identity) is written once in full and as a `$ref` elsewhere
* user-defined primitive mappings (e.g. `kotlin.time.Instant` as epoch millis)
* enums, lists, sets, maps and arrays
* *lookups*: objects that already exist at read time (e.g. definitions owned by a singleton) can be written by a key and found again by that key
* targets: JVM (17+), JS (browser and Node.js, with TypeScript definitions) and Wasm/JS

## Modules

| Module                 | Package                                   | Description |
|------------------------|-------------------------------------------|-------------|
| `json`                 | `net.akehurst.kotlin.json`                | A small JSON document model (`JsonDocument`, `JsonValue`, ...), parser (`Json.process`) and builder DSL (`json { ... }`). No dependencies. |
| `hjson`                | `net.akehurst.hjson`                      | The HJSON equivalent (`HJsonDocument`, `HJson.process`, builder DSL), with a parser built on AGL (`net.akehurst.language:agl-processor`). |
| `kserialisation-json`  | `net.akehurst.kotlin.kserialisation.json` | `KSerialiserJson`: converts Kotlin objects to/from JSON. Depends on `json`. |
| `kserialisation-hjson` | `net.akehurst.kotlin.kserialisation.hjson`| `KSerialiserHJson`: converts Kotlin objects to/from HJSON. Depends on `hjson`. |

The `kserialisation-bytes` folder is an unfinished binary format and is not currently part of the build.

## Getting it

Releases are published to Maven Central under the group `net.akehurst.kotlin.kserialisation`
(see the Maven Central badge above for the latest version).

```kotlin
// build.gradle.kts (Kotlin Multiplatform)
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("net.akehurst.kotlin.kserialisation:kserialisation-json:<version>")
            // or, for HJSON
            implementation("net.akehurst.kotlin.kserialisation:kserialisation-hjson:<version>")
        }
    }
}
```

The type model DSL (`typesDomain { ... }`) comes from `net.akehurst.language:agl-processor`, and class registration
(`KotlinxReflect`) from `net.akehurst.kotlinx:kotlinx-reflect`; add those as dependencies if you configure the serialiser from your own code.

## Usage

### 1. Describe your types

Given some classes:

```kotlin
package com.example

enum class Colour { RED, GREEN, BLUE }

class Node(
    val name: String,
    var child: Node?
) {
    var parent: Node? = null   // a back-reference, not owned
    var size: Int = -1
}
```

create a serialiser and give it a type model. `CMP` marks a composite (owned) property, `REF` a reference;
`VAL`/`VAR` mark whether the property is read-only or mutable. Constructor parameters tell the serialiser how to
create an instance when reading.

```kotlin
import net.akehurst.kotlin.kserialisation.json.KSerialiserJson
import net.akehurst.kotlinx.reflect.EnumValuesFunction
import net.akehurst.kotlinx.reflect.KotlinxReflect
import net.akehurst.language.types.builder.typesDomain

val serialiser = KSerialiserJson()

serialiser.configureFromTypeModel(typesDomain("", true) {
    namespace("com.example") {
        enum("Colour", emptyList())
        data("Node") {
            constructor_ {
                parameter(setOf(), "name", "String")
                parameter(setOf(), "child", "Node", true)   // nullable
            }
            propertyOf(setOf(CMP, VAL), "name", "String")
            propertyOf(setOf(CMP, VAR), "child", "Node")
            propertyOf(setOf(REF, VAR), "parent", "Node")
            propertyOf(setOf(CMP, VAR), "size", "Integer")
        }
    }
})
serialiser.registerKotlinStdPrimitives()

// make the classes available to kotlinx-reflect, so that instances can be created on read
KotlinxReflect.registerClass("com.example.Node", Node::class)
KotlinxReflect.registerClass("com.example.Colour", Colour::class, Colour::values as EnumValuesFunction)

serialiser.registry.resolveImports()
```

### 2. Write and read

```kotlin
val root = Node("root", Node("leaf", null))
root.size = 5
root.child?.parent = root

// toJson(root, data): 'root' is the object that reference paths are relative to, 'data' is the object to write.
// Normally they are the same.
val jsonText: String = serialiser.toJson(root, root).toFormattedJsonString("  ", "  ")

// read it back; the class of each object is taken from its "$class" entry
// (a target KClass can also be passed, e.g. serialiser.toData(text, Array::class) for arrays)
val copy: Node = serialiser.toData(jsonText)
check(copy.child?.parent === copy)
```

The JSON produced looks like this (the shape is the same for HJSON, written with HJSON syntax):

```json
{
  "$kind" : "$OBJECT",
  "$class" : "com.example.Node",
  "name" : "root",
  "child" : {
    "$kind" : "$OBJECT",
    "$class" : "com.example.Node",
    "name" : "leaf",
    "child" : null,
    "parent" : { "$ref" : "/" },
    "size" : { "$kind" : "$PRIMITIVE", "$class" : "kotlin.Int", "$value" : -1 }
  },
  "parent" : null,
  "size" : { "$kind" : "$PRIMITIVE", "$class" : "kotlin.Int", "$value" : 5 }
}
```

`Boolean` and `String` are written as plain JSON values; other numeric types are written as `$PRIMITIVE` objects so that
their exact Kotlin type is preserved.

For HJSON use `KSerialiserHJson` in the same way, with `toHJson(root, data)` and `toHJsonString()`:

```kotlin
val serialiser = KSerialiserHJson()
// ... configure as above ...
val hjsonText = serialiser.toHJson(root, root).toHJsonString()
val copy: Node = serialiser.toData(hjsonText)
```

### Custom primitives

Register a mapping for any type declared as a `primitive` in the type model:

```kotlin
// in the type model:   primitive("Instant")
serialiser.registerPrimitiveAsObject(
    Instant::class,
    { value -> JsonNumber(value.toEpochMilliseconds().toString()) },
    { json -> Instant.fromEpochMilliseconds(json.asNumber().toLong()) }
)
```

`registerPrimitiveAsObject` writes the value wrapped in a `$PRIMITIVE` object (keeping its type name);
`registerPrimitive` writes the raw JSON value only.

### Lookups

Objects that already exist when the data is read (for example, built-in definitions) can be written by a key instead of
in full, and resolved to the existing instance on read:

```kotlin
import net.akehurst.kotlinx.komposite.common.LookupMapper

serialiser.registry.registerLookup(LookupMapper(
    name = "known",
    toKey = { v -> if (v === knownInstance) "the-known-one" else null },
    fromKey = { k -> if (k == "the-known-one") knownInstance else null }
))
```

Such objects are written as `{ "$kind" : "$LOOKUP", "$class" : "known", "$value" : "the-known-one" }`.

### Format reference

| Key / value                | Meaning |
|----------------------------|---------|
| `$kind`                    | one of `$OBJECT`, `$PRIMITIVE`, `$ENUM`, `$LIST`, `$SET`, `$MAP`, `$ARRAY`, `$SINGLETON`, `$SPECIAL`, `$LOOKUP` |
| `$class`                   | qualified type name (or lookup name for `$LOOKUP`) |
| `$value`                   | the value of a primitive, enum, value type or lookup key |
| `$elements`                | the elements of a list, set or array |
| `$entries`                 | the entries of a map, each an object with `$key` and `$value` |
| `{ "$ref" : "/a/b" }`      | a reference to the object written at that path in the document (`/` is the root) |
| `{ $ref : "#/a/b" }`       | the HJSON form of a reference (paths are prefixed with `#/`) |

Objects are compared by identity: two distinct but `equal` objects are both written in full.
A cycle of composite properties that passes through constructor arguments cannot be re-created and gives a
`KSerialiserJsonException` on read.

## Building

The Gradle build is in the `net.akehurst.kotlin.kserialisation` sub-folder:

```shell
cd net.akehurst.kotlin.kserialisation
./gradlew build
```

Requirements: a JDK 17 or newer. Versions of Kotlin and dependencies are in
[`gradle/libs.versions.toml`](net.akehurst.kotlin.kserialisation/gradle/libs.versions.toml).
Snapshot versions of the `net.akehurst.*` dependencies are resolved from `mavenLocal()`, so when the build uses
`-SNAPSHOT` dependencies they must be built and published locally first (`./gradlew publishToMavenLocal` in their repositories).

## License

Apache License 2.0, see [LICENSE.txt](LICENSE.txt).
