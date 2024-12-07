plugins {
    war
}

repositories {
    mavenCentral()
}

description = "PDF Generator."

plugins.findPlugin(JavaPlugin::class)?.let {
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "EUC-JP"
}

tasks.war {
    archiveBaseName.set("spiral_pdf_generator")
}

object Versions {
    val tomcat = "9.0.31"
    val jsonLib = "2.4"
    val commonsFileupload = "1.4"
    val commonsIo = "2.4"
    val iTextPdf = "5.1.0"
    val iTextAsian = "5.1.0"
    val bouncyCastl = "1.46"
}

dependencies {
    compileOnly("org.apache.tomcat:tomcat-servlet-api:${Versions.tomcat}")
    implementation("net.sf.json-lib:json-lib:${Versions.jsonLib}:jdk15")
    implementation("commons-io:commons-io:${Versions.commonsIo}")
    implementation("commons-fileupload:commons-fileupload:${Versions.commonsFileupload}")
    implementation("com.itextpdf:itextpdf:${Versions.iTextPdf}")
    implementation("com.itextpdf:itext-asian:${Versions.iTextAsian}")
    implementation("org.bouncycastle:bcprov-jdk15:${Versions.bouncyCastl}")
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
        }
    }
}

