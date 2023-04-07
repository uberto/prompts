plugins {
    kotlin("jvm") version "1.8.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(platform("org.http4k:http4k-bom:4.41.3.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-jetty")
    implementation("org.http4k:http4k-client-okhttp")
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

//application {
//    mainClass.set("com.example.http4kdemo.Http4k_vanillaKt")
//}

application {
    mainClass.set("MainKt")
}