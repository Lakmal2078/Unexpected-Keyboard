package buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
abstract class InitDebugKeystore : DefaultTask() {
  @get:OutputFile
  abstract val keystoreFile: RegularFileProperty

  @TaskAction
  fun ensureKeystore() {
    val keystore = keystoreFile.get().asFile
    val valid = keystore.isFile && try {
      ProcessBuilder(
        "keytool", "-list", "-keystore", keystore.absolutePath,
        "-storepass", "android", "-alias", "androiddebugkey"
      ).redirectErrorStream(true).start().waitFor() == 0
    } catch (_: Exception) {
      false
    }

    if (!valid) {
      if (keystore.exists() && !keystore.delete()) {
        throw GradleException("Could not remove invalid debug keystore: ${keystore.absolutePath}")
      }
      keystore.parentFile?.mkdirs()
      val process = ProcessBuilder(
        "keytool", "-genkeypair", "-noprompt",
        "-dname", "cn=Debug, ou=Build, o=Android, c=US",
        "-alias", "androiddebugkey", "-keypass", "android",
        "-keystore", keystore.absolutePath, "-keyalg", "RSA",
        "-storepass", "android", "-validity", "10000"
      ).inheritIO().start()
      if (process.waitFor() != 0) {
        throw GradleException("Failed to generate debug keystore")
      }
    }
  }
}
