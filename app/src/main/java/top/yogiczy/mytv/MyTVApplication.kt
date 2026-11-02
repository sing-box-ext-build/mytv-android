package top.yogiczy.mytv

import android.app.Application
import android.util.Log
import top.yogiczy.mytv.ui.utils.SP
import java.io.File

class MyTVApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        UnsafeTrustManager.enableUnsafeTrustManager()
        AppGlobal.cacheDir = applicationContext.cacheDir
        SP.init(applicationContext)

        // 1. 启动 tv-go 代理核心
        startTvGo()

        // 2. 只要不是我们自己的本地代理，或者是官方硬编码的源，全部强制覆盖
        if (SP.iptvSourceUrl != "http://127.0.0.1:3000/live.m3u") {
            SP.iptvSourceUrl = "http://127.0.0.1:3000/live.m3u"
        }
    }

    private fun startTvGo() {
        try {
            val nativeDir = applicationInfo.nativeLibraryDir
            val sourceFile = File("$nativeDir/libtvgo.so")
            
            // 目标路径：拷贝到 App 私有 files 目录
            val targetFile = File(applicationContext.filesDir, "tv-go-bin")
            
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                Log.d("TvGo", "二进制文件拷贝成功: ${targetFile.absolutePath}")
            } else {
                Log.e("TvGo", "找不到二进制文件: ${sourceFile.absolutePath}")
                return
            }

            // 【关键修复】使用 sh -c 来确保 chmod 真的生效
            try {
                val chmodProcess = Runtime.getRuntime().exec(arrayOf("sh", "-c", "chmod 755 ${targetFile.absolutePath}"))
                val exitCode = chmodProcess.waitFor()
                Log.d("TvGo", "chmod 执行完成，退出码: $exitCode")
            } catch (e: Exception) {
                Log.w("TvGo", "chmod 提权失败: ${e.message}")
            }

            // ==========================================
            // 【核心修复】注入环境变量，把可写目录传给 Go 程序
            // ==========================================
            val processBuilder = ProcessBuilder(targetFile.absolutePath)
            
            // 这里把 App 的私有 cache 目录传进去
            processBuilder.environment()["TV_GO_TMP_DIR"] = applicationContext.cacheDir.absolutePath
            
            val process = processBuilder.start()
            Log.i("TvGo", "tv-go 启动成功，路径: ${targetFile.absolutePath}")

            // 读取日志
            Thread {
                try {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { Log.d("TvGo-Output", it) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

        } catch (e: Exception) {
            Log.e("TvGo", "启动 tv-go 异常", e)
        }
    }
}