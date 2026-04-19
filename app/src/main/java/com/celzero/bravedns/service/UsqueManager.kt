package com.celzero.bravedns.service

import Logger
import Logger.LOG_TAG_PROXY
import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UsqueManager {
    const val SOCKS_HOST = "127.0.0.1"
    const val SOCKS_PORT = 40000
    private const val BINARY_NAME = "usque-rs-arm64"
    private var process: Process? = null

    fun isRegistered(ctx: Context): Boolean {
        return File(ctx.filesDir, "config.json").exists()
    }


The file is a complete mess — two versions of `registerWithWarp`, one using `Log`/`TAG`/`extractBinary`/`CONFIG_DIR` (none of which exist), one commented out, dead code everywhere. Here is the clean final version — **replace the entire file with this**:

```kotlin
package com.celzero.bravedns.service

import Logger
import Logger.LOG_TAG_PROXY
import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UsqueManager {
    const val SOCKS_HOST = "127.0.0.1"
    const val SOCKS_PORT = 40000
    private const val BINARY_NAME = "usque-rs-arm64"
    private var process: Process? = null

    fun isRegistered(ctx: Context): Boolean {
        val f = File(ctx.filesDir, "config.json")
        Logger.i(LOG_TAG_PROXY, "isRegistered: ${f.absolutePath} exists=${f.exists()}")
        return f.exists()
    }

    suspend fun registerWithWarp(context: Context): Boolean = withContext(Dispatchers.IO) {
        Logger.i(LOG_TAG_PROXY, "registerWithWarp: CALLED")
        try {
            val bin = copyBinary(context)
            Logger.i(LOG_TAG_PROXY, "registerWithWarp: bin=${bin.absolutePath} canExec=${bin.canExecute()} exists=${bin.exists()}")

            val configFile = File(context.filesDir, "config.json")
            val cmd = listOf(bin.absolutePath, "register", "-c", configFile.absolutePath)
            Logger.i(LOG_TAG_PROXY, "registerWithWarp: cmd=${cmd.joinToString(" ")}")

            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()

            // Answer "y" to the Terms of Service interactive prompt
            proc.outputStream.bufferedWriter().let { w ->
                w.write("y\n")
                w.flush()
                w.close()
            }

            val output = proc.inputStream.bufferedReader().readText()
            val exit = proc.waitFor()

            Logger.i(LOG_TAG_PROXY, "registerWithWarp: exit=$exit")
            Logger.i(LOG_TAG_PROXY, "registerWithWarp: output=$output")
            Logger.i(LOG_TAG_PROXY, "registerWithWarp: configExists=${configFile.exists()} size=${configFile.length()}")

            val ok = exit == 0 && configFile.exists() && configFile.length() > 0L
            Logger.i(LOG_TAG_PROXY, "registerWithWarp: result=$ok")
            ok
        } catch (e: Exception) {
            Logger.e(LOG_TAG_PROXY, "registerWithWarp: EXCEPTION ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }

    suspend fun startSocksProxy(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        Logger.i(LOG_TAG_PROXY, "startSocksProxy: CALLED")
        stopSocksProxy()
        try {
            val bin = copyBinary(ctx)
            Logger.i(LOG_TAG_PROXY, "startSocksProxy: bin=${bin.absolutePath} canExec=${bin.canExecute()}")

            process = ProcessBuilder(
                bin.absolutePath, "socks",
                "-b", SOCKS_HOST,
                "-p", SOCKS_PORT.toString()
            ).redirectErrorStream(true).start()

            Thread.sleep(800)
            val alive = process?.isAlive == true
            Logger.i(LOG_TAG_PROXY, "startSocksProxy: alive=$alive")
            alive
        } catch (e: Exception) {
            Logger.e(LOG_TAG_PROXY, "startSocksProxy: EXCEPTION ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }

    fun stopSocksProxy() {
        Logger.i(LOG_TAG_PROXY, "stopSocksProxy: called")
        process?.destroy()
        process = null
    }

    fun isRunning(): Boolean = process?.isAlive == true

    private fun copyBinary(ctx: Context): File {
        val out = File(ctx.filesDir, BINARY_NAME)
        Logger.i(LOG_TAG_PROXY, "copyBinary: path=${out.absolutePath} exists=${out.exists()}")
        if (!out.exists()) {
            Logger.i(LOG_TAG_PROXY, "copyBinary: extracting from assets")
            ctx.assets.open(BINARY_NAME).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            out.setExecutable(true)
            Logger.i(LOG_TAG_PROXY, "copyBinary: done canExec=${out.canExecute()}")
        }
        return out
    }
}

/**Key fixes: single `registerWithWarp`, uses `copyBinary` consistently, answers `y\n` to the ToS prompt, logs everything with `Logger` (the app's own logger), records exceptions to Crashlytics, correct `socks` command args matching usque's actual CL
I. **/


    

/** suspend fun registerWithWarp(context: Context): Boolean = withContext(Dispatchers.IO) {
    Logger.i(LOG_TAG_PROXY, "registerWithWarp CALLED")  // add this as first line
    try {
        val bin = copyBinary(context)
        Logger.i(LOG_TAG_PROXY, "usque register: path=${bin.absolutePath} canExec=${bin.canExecute()}")

        val configFile = File(context.filesDir, "config.json")
        val cmd = listOf(bin.absolutePath, "register", "-c", configFile.absolutePath)
        Logger.i(LOG_TAG_PROXY, "usque register cmd: ${cmd.joinToString(" ")}")

        val proc = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()

        // Answer "y" to the Terms of Service prompt
        proc.outputStream.bufferedWriter().use { it.write("y\n"); it.flush() }

        val output = proc.inputStream.bufferedReader().readText()
        val exit = proc.waitFor()

        Logger.i(LOG_TAG_PROXY, "usque register exit=$exit output=$output")
        Logger.i(LOG_TAG_PROXY, "config exists=${configFile.exists()} size=${configFile.length()}")

        exit == 0 && configFile.exists() && configFile.length() > 0L
    } catch (e: Exception) {
        Logger.e(LOG_TAG_PROXY, "usque register failed: ${e.message}", e)
        false
    }
}. **/

    

    suspend fun startSocksProxy(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        stopSocksProxy()
        return@withContext try {
            val bin = copyBinary(ctx)
            process = ProcessBuilder(
                bin.absolutePath, "socks5",
                "--host", SOCKS_HOST,
                "--port", SOCKS_PORT.toString()
            ).redirectErrorStream(true).start()
            Thread.sleep(800)
            process?.isAlive == true
        } catch (e: Exception) {
            Logger.e(LOG_TAG_PROXY, "usque start failed: ${e.message}", e)
            false
        }
    }

    fun stopSocksProxy() {
        process?.destroy()
        process = null
    }

    private fun copyBinary(ctx: Context): File {
        val out = File(ctx.filesDir, BINARY_NAME)
        if (!out.exists()) {
            ctx.assets.open(BINARY_NAME).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            out.setExecutable(true)
        }
        return out
    }
}
