import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

plugins {
  id("com.android.application") version "8.13.2"
}

dependencies {
  // Following versions of androidx.window require sdk version 23
  implementation("androidx.window:window-java:1.4.0")
  implementation("androidx.core:core:1.16.0") // Version 1.17.0 available with sdk 36
  implementation("androidx.customview:customview:1.2.0") // Virtual accessibility nodes for the custom keyboard
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito:mockito-core:5.14.2")
  androidTestImplementation("androidx.test:core:1.6.1")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test:runner:1.6.2")
}



android {
  namespace = "juloo.keyboard2"
  compileSdkVersion = "android-36"

  defaultConfig {
    applicationId = "juloo.keyboard2"
    minSdk = 21
    targetSdk { version = release(36) }
    versionCode = 57
    versionName = "2.1.1"
    ndk.abiFilters.addAll(listOf("x86_64", "arm64-v8a"))
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  sourceSets {
    named("main") {
      manifest.srcFile("AndroidManifest.xml")
      java.srcDirs("srcs/java")
      res.srcDirs("res", "build/generated-resources")
      assets.srcDirs("assets")
      jniLibs.srcDirs("libs")
    }

    named("test") {
      java.srcDirs("test")
      resources.srcDirs("test/resources")
    }
  }

  // externalNativeBuild {
  //   ndkBuild {
  //     path = file("vendor/Android.mk")
  //   }
  // }

  signingConfigs {
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }

    create("release") {
      val ks = System.getenv("RELEASE_KEYSTORE")
      if (ks != null) {
        storeFile = file(ks)
        storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("RELEASE_KEY_ALIAS")
        keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    named("release") {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro")
      isShrinkResources = true
      isDebuggable = false
      resValue("string", "app_name", "@string/app_name_release")
      signingConfig = signingConfigs["release"]
      ndk {
        debugSymbolLevel = "none"
      }
    }

    named("debug") {
      isMinifyEnabled = false
      isShrinkResources = false
      isDebuggable = true
      applicationIdSuffix = ".debug"
      resValue("string", "app_name", "@string/app_name_debug")
      resValue("bool", "debug_logs", "true")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  packaging {
    jniLibs {
      keepDebugSymbols.add("**/*.so")
    }
  }
}

val buildKeyboardFont by tasks.registering(Exec::class) {
  val `in` = projectDir.resolve("srcs/special_font")
  val out = layout.projectDirectory.file("assets/special_font.ttf")
  inputs.dir(`in`)
  outputs.file(out)
  doFirst { println("\nBuilding assets/special_font.ttf") }
  workingDir = `in`
  val svgFiles = `in`.listFiles()!!.filter {
    it.isFile && it.name.endsWith(".svg")
  }.toTypedArray()
  commandLine("fontforge", "-lang=ff", "-script", "build.pe", out.asFile.absolutePath, *svgFiles)
}

val genEmojis by tasks.registering(Exec::class) {
  doFirst { println("\nGenerating res/raw/emojis.txt") }
  workingDir = projectDir
  commandLine("python3", "gen_emoji.py")
}

val genLayoutsList by tasks.registering(Exec::class) {
  inputs.dir(projectDir.resolve("srcs/layouts"))
  outputs.file(projectDir.resolve("res/values/layouts.xml"))
  doFirst { println("\nGenerating res/values/layouts.xml") }
  workingDir = projectDir
  commandLine("python3", "gen_layouts.py")
}

val genMethodXml by tasks.registering(Exec::class) {
  val out = projectDir.resolve("res/xml/method.xml")
  inputs.file(projectDir.resolve("gen_method_xml.py"))
  inputs.file(projectDir.resolve("res/values/dictionaries.xml"))
  outputs.file(out)
  doFirst { println("\nGenerating res/xml/method.xml") }
  doFirst { standardOutput = FileOutputStream(out) }
  workingDir = projectDir
  commandLine("python3", "gen_method_xml.py")
}

val checkKeyboardLayouts by tasks.registering(Exec::class) {
  inputs.dir(projectDir.resolve("srcs/layouts"))
  inputs.file(projectDir.resolve("srcs/java/juloo/keyboard2/KeyValue.java"))
  outputs.file(projectDir.resolve("check_layout.output"))
  doFirst { println("\nChecking layouts") }
  workingDir = projectDir
  commandLine("python3", "check_layout.py")
}

val compileComposeSequences by tasks.registering(Exec::class) {
  val `in` = projectDir.resolve("srcs/compose")
  val out = projectDir.resolve("srcs/java/juloo/keyboard2/ComposeKeyData.java")
  inputs.dir(`in`)
  outputs.file(out)
  doFirst { println("\nGenerating $out") }
  val sequences = `in`.listFiles { it: File ->
    !it.name.endsWith(".py") && !it.name.endsWith(".md")
  }!!.map { it.absolutePath }.toTypedArray()
  workingDir = projectDir
  commandLine("python3", `in`.resolve("compile.py").absolutePath, *sequences)
  doFirst { standardOutput = FileOutputStream(out) }
}

tasks.withType(Test::class).configureEach {
  dependsOn(genLayoutsList, checkKeyboardLayouts, compileComposeSequences, genMethodXml)
}

val initDebugKeystore by tasks.registering(Exec::class) {
  // Automatically generate debug keystore if it doesn't exist (e.g. in CI / GitHub Actions)
  isEnabled = !file("debug.keystore").exists()
  commandLine("keytool", "-genkeypair", "-noprompt", "-dname", "cn=Debug, ou=Build, o=Android, c=US", "-alias", "androiddebugkey", "-keypass", "android", "-keystore", "debug.keystore", "-keyalg", "RSA", "-storepass", "android", "-validity", "10000")
}

// latn_qwerty_us is used as a raw resource by the custom layout option.
val copyRawQwertyUS by tasks.registering(Copy::class) {
  from("srcs/layouts/latn_qwerty_us.xml")
  into("build/generated-resources/raw")
}

val copyLayoutDefinitions by tasks.registering(Copy::class) {
  from("srcs/layouts")
  include("*.xml")
  into("build/generated-resources/xml")
}

tasks.named("preBuild") {
  dependsOn(initDebugKeystore, copyRawQwertyUS, copyLayoutDefinitions)
  // 'mustRunAfter' defines ordering between tasks (which is required by
  // Gradle) but doesn't create a dependency. These rules update files that are
  // checked in the repository that don't need to be updated during regular
  // builds.
  mustRunAfter(genEmojis, genLayoutsList, compileComposeSequences, genMethodXml)
}

val copyDebugApkToAppPath by tasks.registering(Copy::class) {
  from(layout.buildDirectory.dir("outputs/apk/debug"))
  include("Unexpected-Keyboard-debug.apk")
  into(rootProject.layout.projectDirectory.dir("app/build/outputs/apk/debug"))
  rename { "app-debug.apk" }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
  finalizedBy(copyDebugApkToAppPath)
}

val generateReleaseApkReport by tasks.registering {
  group = "reporting"
  description = "Generates a size and file count report for the release APK output."

  val releaseOutputDir = layout.buildDirectory.dir("outputs/apk/release")
  val reportOutputFile = layout.buildDirectory.file("reports/apk-release-report.txt")

  outputs.upToDateWhen { false }

  doLast {
    val dir = releaseOutputDir.get().asFile
    if (!dir.exists() || !dir.isDirectory) {
      logger.warn("Release output directory does not exist: ${dir.absolutePath}")
      return@doLast
    }

    val allFiles = dir.walkTopDown().filter { it.isFile }.toList()
    val apkFiles = allFiles.filter { it.extension == "apk" }

    val reportFile = reportOutputFile.get().asFile
    reportFile.parentFile?.mkdirs()

    val reportContent = buildString {
      appendLine("=========================================")
      appendLine("        Release APK Build Report         ")
      appendLine("=========================================")
      appendLine("Generated At: ${Date()}")
      appendLine("Output Directory: ${dir.absolutePath}")
      appendLine("Total Files in Output: ${allFiles.size}")
      appendLine()
      appendLine("--- APK Files ---")
      if (apkFiles.isEmpty()) {
        appendLine("No APK file found in output directory.")
      } else {
        apkFiles.forEach { apk ->
          val sizeBytes = apk.length()
          val sizeKb = String.format(Locale.US, "%.2f KB", sizeBytes / 1024.0)
          val sizeMb = String.format(Locale.US, "%.2f MB", sizeBytes / (1024.0 * 1024.0))
          appendLine("File: ${apk.name}")
          appendLine("Size: $sizeBytes bytes ($sizeKb / $sizeMb)")
        }
      }
      appendLine()
      appendLine("--- All Output Files ---")
      allFiles.forEach { file ->
        val relPath = file.relativeTo(dir).path
        appendLine("- $relPath (${file.length()} bytes)")
      }
      appendLine("=========================================")
    }

    reportFile.writeText(reportContent)
    println("\n$reportContent\nReport saved to: ${reportFile.absolutePath}")
  }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
  finalizedBy(generateReleaseApkReport)
}


