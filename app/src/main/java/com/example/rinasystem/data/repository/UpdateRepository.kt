package com.example.rinasystem.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.rinasystem.BuildConfig
import com.example.rinasystem.data.api.AriaApi
import com.example.rinasystem.data.local.ServerConfigManager
import com.example.rinasystem.data.model.AppVersionResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val api: AriaApi,
    private val okHttpClient: OkHttpClient,
    private val serverConfigManager: ServerConfigManager,
    @ApplicationContext private val context: Context,
) {
    suspend fun checkForUpdate(): ApiResult<AppVersionResponse> {
        return try {
            val response = api.getLatestVersion()
            if (response.isSuccessful) {
                val versionInfo = response.body()!!
                ApiResult.Success(versionInfo)
            } else {
                ApiResult.Error("ไม่สามารถตรวจสอบเวอร์ชันได้")
            }
        } catch (e: Exception) {
            ApiResult.Error("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้")
        }
    }

    fun isUpdateAvailable(versionInfo: AppVersionResponse): Boolean {
        val installedCode = try {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
        } catch (_: Exception) {
            BuildConfig.VERSION_CODE
        }
        android.util.Log.i("UpdateRepo", "Compare: server=${versionInfo.versionCode} > installed=$installedCode")
        return versionInfo.versionCode > installedCode
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Int) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = serverConfigManager.getServerUrl()
            val fullUrl = "$baseUrl$downloadUrl"

            val request = Request.Builder().url(fullUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val totalBytes = body.contentLength()

            val updatesDir = File(context.getExternalFilesDir(null), "updates")
            updatesDir.mkdirs()

            val apkFile = File(updatesDir, "aria-update.apk")
            apkFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                body.byteStream().use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (totalBytes > 0) {
                            val progress = (bytesRead * 100 / totalBytes).toInt()
                            onProgress(progress.coerceIn(0, 100))
                        }
                    }
                }
            }

            apkFile
        } catch (e: Exception) {
            null
        }
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
