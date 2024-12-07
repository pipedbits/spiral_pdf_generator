plugins {
    war
}

repositories {
    mavenCentral()
}

description = "Sample Client of PDF Generator."

plugins.findPlugin(JavaPlugin::class)?.let {
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.war {
    archiveBaseName.set("spiral_pdf_sample")
}

object Versions {
    val tomcat = "9.0.31"
    val jsonLib = "2.4"
    val commonsFileupload = "1.4"
    val okhttp = "4.11.0"
}

dependencies {
    compileOnly("org.apache.tomcat:tomcat-servlet-api:${Versions.tomcat}")
    implementation("net.sf.json-lib:json-lib:${Versions.jsonLib}:jdk15")
    implementation("commons-fileupload:commons-fileupload:${Versions.commonsFileupload}")
    implementation("com.squareup.okhttp3:okhttp:${Versions.okhttp}")
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
        }
    }
}

