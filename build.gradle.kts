import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileOutputStream

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

val debugKeystorePassword = System.getenv("DEBUG_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val debugKeyPassword = System.getenv("DEBUG_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
val isCi = listOf("CI", "GITHUB_ACTIONS").any {
  System.getenv(it).equals("true", ignoreCase = true)
}

// Dependency inspection, unit tests, and lint do not sign APKs and must remain
// runnable in CI without signing secrets. Require CI credentials only for tasks
// that package an APK/AAB using the debug signing config.
val requestedTasks = gradle.startParameter.taskNames.map { it.substringAfterLast(":") }
val needsDebugSigning = requestedTasks.any { task ->
  task.equals("assemble", ignoreCase = true)
      || task.equals("build", ignoreCase = true)
      || (task.contains("debug", ignoreCase = true)
          && (task.contains("assemble", ignoreCase = true)
              || task.contains("package", ignoreCase = true)
              || task.contains("bundle", ignoreCase = true)
              || task.contains("install", ignoreCase = true)))
}

if (isCi && needsDebugSigning) {
  val missingPasswords = buildList {
    if (debugKeystorePassword == null) add("DEBUG_KEYSTORE_PASSWORD")
    if (debugKeyPassword == null) add("DEBUG_KEY_PASSWORD")
  }
  if (missingPasswords.isNotEmpty()) {
    throw GradleException(
      "Missing required CI debug signing environment variable(s): ${missingPasswords.joinToString()}"
    )
  }
} else if (!isCi && (debugKeystorePassword == null || debugKeyPassword == null)) {
  logger.warn(
    "Debug signing passwords are unset; using the local-only default password 'debug0'."
  )
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
      java.srcDirs("srcs/java", "vendor/cdict/java/juloo.cdict")
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
    named("debug") {
      storeFile = file(System.getenv("DEBUG_KEYSTORE") ?: "debug.keystore")
      storePassword = debugKeystorePassword ?: "debug0"
      keyAlias = System.getenv("DEBUG_KEY_ALIAS") ?: "debug"
      keyPassword = debugKeyPassword ?: "debug0"
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
      signingConfig = signingConfigs["debug"]
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


// This raises an error with an informative message instead of the confusing
// ndk-build errors that occur when submodules are not initialized.
gradle.projectsEvaluated {
  if (!file("vendor/cdict/java").exists())
    throw GradleException(
      "Git submodules are not initialized. Please run: 'git submodule update --init --recursive'\n" +
      "For more details, see the Build Prerequisites section in README.md and CONTRIBUTING.md."
    )
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
  commandLine("python", "gen_emoji.py")
}

val genLayoutsList by tasks.registering(Exec::class) {
  inputs.dir(projectDir.resolve("srcs/layouts"))
  outputs.file(projectDir.resolve("res/values/layouts.xml"))
  doFirst { println("\nGenerating res/values/layouts.xml") }
  workingDir = projectDir
  commandLine("python", "gen_layouts.py")
}

val genMethodXml by tasks.registering(Exec::class) {
  val out = projectDir.resolve("res/xml/method.xml")
  inputs.file(projectDir.resolve("gen_method_xml.py"))
  inputs.file(projectDir.resolve("res/values/dictionaries.xml"))
  outputs.file(out)
  doFirst { println("\nGenerating res/xml/method.xml") }
  doFirst { standardOutput = FileOutputStream(out) }
  workingDir = projectDir
  commandLine("python", "gen_method_xml.py")
}

val checkKeyboardLayouts by tasks.registering(Exec::class) {
  inputs.dir(projectDir.resolve("srcs/layouts"))
  inputs.file(projectDir.resolve("srcs/java/juloo/keyboard2/KeyValue.java"))
  outputs.file(projectDir.resolve("check_layout.output"))
  doFirst { println("\nChecking layouts") }
  workingDir = projectDir
  commandLine("python", "check_layout.py")
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
  commandLine("python", `in`.resolve("compile.py").absolutePath, *sequences)
  doFirst { standardOutput = FileOutputStream(out) }
}

tasks.withType(Test::class).configureEach {
  dependsOn(genLayoutsList, checkKeyboardLayouts, compileComposeSequences, genMethodXml)
}

val initDebugKeystore by tasks.registering(Exec::class) {
  doFirst { println("Initializing local-only debug keystore (not for production releases)") }
  // Only auto-generate for local development environments when keystore is missing and not in CI
  isEnabled = !file("debug.keystore").exists() && !isCi
  commandLine("keytool", "-genkeypair", "-dname", "cn=d, ou=e, o=b, c=ug", "-alias", "debug", "-keypass", "debug0", "-keystore", "debug.keystore", "-keyalg", "rsa", "-storepass", "debug0", "-validity", "10000")
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
