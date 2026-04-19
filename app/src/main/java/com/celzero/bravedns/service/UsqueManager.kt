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
    private const val BINARY_NAME = "usque-rs-arm32"
    private var process: Process? = null

    fun isRegistered(ctx: Context): Boolean {
        return File(ctx.filesDir, "config.json").exists()
    }

/**    suspend fun registerWithWarp(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val bin = copyBinary(ctx)
            if (bin == null) {
            Log.e(TAG, "registerWithWarp: binary is null — asset not found or extraction failed")
            return@withContext false
        }
        /**    val proc = ProcessBuilder(bin.absolutePath, "register")
                .redirectErrorStream(true)
                .start()
            proc.waitFor(30, TimeUnit.SECONDS) && proc.exitValue() == 0
        } catch (e: Exception) {
            Logger.e(LOG_TAG_PROXY, "usque register failed: ${e.message}", e)
            false
        }**/

                val configFile = ensureConfigFile(context)
        Log.i(TAG, "registerWithWarp: config path=${configFile.absolutePath}")

        val cmd = listOf(bin.absolutePath, "register", "-c", configFile.absolutePath)
        Log.i(TAG, "registerWithWarp: running cmd=${cmd.joinToString(" ")}")

        val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = proc.inputStream.bufferedReader().readText()
        val exit = proc.waitFor()

        Log.i(TAG, "registerWithWarp: exit=$exit output=$output configExists=${configFile.exists()} configSize=${configFile.length()}")

        exit == 0 && configFile.exists() && configFile.length() > 0L
    } catch (e: Exception) {
        Log.e(TAG, "registerWithWarp failed: ${e.message}", e)
        false
    } **/





   /**     val configFile = ensureConfigFile(context)
        Log.i(TAG, "registerWithWarp: config path=${configFile.absolutePath}")

        val cmd = listOf(bin.absolutePath, "register", "-c", configFile.absolutePath)
        Log.i(TAG, "registerWithWarp: running cmd=${cmd.joinToString(" ")}")

        val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = proc.inputStream.bufferedReader().readText()
        val exit = proc.waitFor()

        Log.i(TAG, "registerWithWarp: exit=$exit output=$output configExists=${configFile.exists()} configSize=${configFile.length()}")

        exit == 0 && configFile.exists() && configFile.length() > 0L
    } catch (e: Exception) {
        Log.e(TAG, "registerWithWarp failed: ${e.message}", e)
        false
    }**/
    }



    suspend fun registerWithWarp(context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val bin = extractBinary(context)
        if (bin == null) {
            Log.e(TAG, "registerWithWarp: binary is null — asset not found or extraction failed")
            return@withContext false
        }
        Log.i(TAG, "registerWithWarp: binary path=${bin.absolutePath} exists=${bin.exists()} canExecute=${bin.canExecute()}")
        
        val configFile = ensureConfigFile(context)
        Log.i(TAG, "registerWithWarp: config path=${configFile.absolutePath}")

        val cmd = listOf(bin.absolutePath, "register", "-c", configFile.absolutePath)
        Log.i(TAG, "registerWithWarp: running cmd=${cmd.joinToString(" ")}")

        val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = proc.inputStream.bufferedReader().readText()
        val exit = proc.waitFor()

        Log.i(TAG, "registerWithWarp: exit=$exit output=$output configExists=${configFile.exists()} configSize=${configFile.length()}")

        exit == 0 && configFile.exists() && configFile.length() > 0L
    } catch (e: Exception) {
        Log.e(TAG, "registerWithWarp failed: ${e.message}", e)
        false
    }
}

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
