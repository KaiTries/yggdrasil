plugins {
  java
  `java-library`
  checkstyle
  pmd
  alias(libs.plugins.spotbugs)
  jacoco
}

checkstyle {
  config = resources.text.fromFile("${rootProject.projectDir}/checkstyle.xml")
  toolVersion = libs.versions.checkstyle.get()
}

pmd {
  toolVersion = libs.versions.pmd.get()
  ruleSetConfig = resources.text.fromFile("${rootProject.projectDir}/pmd.xml")
}

spotbugs {
  toolVersion = libs.versions.spotbugs
}

jacoco {
  toolVersion = libs.versions.jacoco.get()
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}


dependencies {
  implementation(libs.vertx.core)

  implementation(libs.rdf4j.model)
  implementation(libs.rdf4j.rio.turtle)
  implementation(libs.rdf4j.rio.jsonld)

  implementation(libs.gson)
  implementation(libs.wot.td.java)


  implementation(files("../libs/bindings-1.0-SNAPSHOT.jar"))
  implementation(files("../libs/core-1.0-SNAPSHOT.jar"))
  implementation(files("../libs/interaction-1.0-SNAPSHOT.jar"))
  implementation("org.eclipse.rdf4j:rdf4j-runtime:3.7.4@pom") {
    isTransitive = true
  }
  implementation("com.google.guava:guava:11.0.2")
  implementation("io.vavr:vavr:0.10.4")
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("org.apache.httpcomponents.client5:httpclient5:5.2.2")
  implementation("org.apache.httpcomponents.core5:httpcore5:5.2.2")


  compileOnly(libs.spotbugs.annotations)
  pmd(libs.pmd.java)
  pmd(libs.pmd.ant)

  testImplementation(platform(libs.junit.platform))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.vertx.junit5)

  testCompileOnly(libs.spotbugs.annotations)
}

tasks {
  test {
    useJUnitPlatform()
    finalizedBy(jacocoTestReport)
  }

  spotbugsMain {
    reports.create("html") {
        required.set(true)
    }
  }
  spotbugsTest {
    reports.create("html") {
        required.set(true)
    }
  }
}
