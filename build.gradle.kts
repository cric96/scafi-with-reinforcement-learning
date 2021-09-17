import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Scala version
val scalaLib: String by project
val scala: String by project
val batch: String by project

plugins {
    java
    scala
    id("com.github.johnrengelman.shadow") version "4.0.3"
    idea
    kotlin("jvm") version "1.3.50"
}

repositories {
    mavenCentral()
}

dependencies {
    // Alchemist dependency
    implementation("it.unibo.alchemist:alchemist:_")
    implementation("it.unibo.alchemist:alchemist-incarnation-scafi:_")
    implementation("it.unibo.alchemist:alchemist-swingui:_")
    // ScaFi dependency
    implementation("org.scala-lang:scala-library:$scalaLib")
    implementation("it.unibo.scafi:scafi-core_${scala}:_")
}

tasks.withType<ScalaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

fun makeTest(
        file: String,
        name: String = file,
        sampling: Double = 1.0,
        time: Double = Double.POSITIVE_INFINITY,
        vars: Set<String> = setOf(),
        maxHeap: Long? = null,
        taskSize: Int = 1024,
        threads: Int? = null,
        debug: Boolean = false
) {
    val heap: Long = maxHeap ?: if (System.getProperty("os.name").toLowerCase().contains("linux")) {
        ByteArrayOutputStream()
                .use { output ->
                    exec {
                        executable = "bash"
                        args = listOf("-c", "cat /proc/meminfo | grep MemAvailable | grep -o '[0-9]*'")
                        standardOutput = output
                    }
                    output.toString().trim().toLong() / 1024
                }
                .also { println("Detected ${it}MB RAM available.") }  * 9 / 10
    } else {
        // Guess 10GB RAM of which 2 used by the OS
        10 * 1024L
    }

    val threadCount = threads ?: maxOf(1, minOf(Runtime.getRuntime().availableProcessors(), heap.toInt() / taskSize ))
    println("Running on $threadCount threads")

    val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    task<JavaExec>("$name") {
        classpath = sourceSets["main"].runtimeClasspath
        classpath("src/main/protelis")
        main = "it.unibo.alchemist.Alchemist"
        maxHeapSize = "${heap}m"
        jvmArgs("-XX:+AggressiveHeap")
        jvmArgs("-XX:-UseGCOverheadLimit")
        //jvmArgs("-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap") // https://stackoverflow.com/questions/38967991/why-are-my-gradle-builds-dying-with-exit-code-137
        if (debug) {
            jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044")
        }
        File("data").mkdirs()
        args(
                "-y", "src/main/yaml/${file}.yml",
                "-t", "$time",
                "-e", "data/${today}-${name}",
                "-p", threadCount,
                "-i", "$sampling"
        )
        if (vars.isNotEmpty() && batch == "true") {
            args("-b", "-var", *vars.toTypedArray())
        }
    }
}
// thread = 1 force simulation to be sequential
makeTest(name="swapSource", file = "swapSource", vars = setOf("episode"), threads = 1)
makeTest(name="plain", file = "plain", vars = setOf("episode"), threads = 1)
