package com.najmi.sciuro.export

import android.content.Context
import android.util.Base64
import app.cash.sqldelight.db.SqlDriver
import com.sciuro.core.ledger.security.DatabaseKeyManager
import java.io.File
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object EncryptedImporter {

    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_SIZE = 256

    suspend fun import(context: Context, passphrase: String, inputStream: InputStream, sqlDriver: SqlDriver): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val allBytes = inputStream.readBytes()

                if (allBytes.size < 8) return@withContext Result.failure(Exception("Invalid backup file — too small"))

                val headerLen = ((allBytes[0].toInt() and 0xFF) shl 24) or
                    ((allBytes[1].toInt() and 0xFF) shl 16) or
                    ((allBytes[2].toInt() and 0xFF) shl 8) or
                    (allBytes[3].toInt() and 0xFF)

                if (headerLen <= 0 || headerLen > allBytes.size - 4) {
                    return@withContext Result.failure(Exception("Invalid backup file — bad header length"))
                }

                val headerJson = String(allBytes, 4, headerLen, Charsets.UTF_8)
                val header = JSONObject(headerJson)

                val magic = header.getString("magic")
                if (magic != "SCIB") return@withContext Result.failure(Exception("Not a Sciuro backup file"))

                val version = header.getInt("version")
                if (version !in 1..2) return@withContext Result.failure(Exception("Unsupported backup version: $version"))

                val salt = Base64.decode(header.getString("salt"), Base64.NO_WRAP)
                val iv = Base64.decode(header.getString("iv"), Base64.NO_WRAP)

                val ciphertextOffset = 4 + headerLen
                val ciphertext = allBytes.copyOfRange(ciphertextOffset, allBytes.size)

                val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE)
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val key = factory.generateSecret(spec).encoded
                val secretKey = SecretKeySpec(key, "AES")

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

                val decrypted = cipher.doFinal(ciphertext)

                val dbBytes: ByteArray
                if (version >= 2) {
                    val keyLen = ((decrypted[0].toInt() and 0xFF) shl 24) or
                        ((decrypted[1].toInt() and 0xFF) shl 16) or
                        ((decrypted[2].toInt() and 0xFF) shl 8) or
                        (decrypted[3].toInt() and 0xFF)

                    if (keyLen > 0 && keyLen <= decrypted.size - 4) {
                        val recoveredKey = decrypted.copyOfRange(4, 4 + keyLen)
                        DatabaseKeyManager.storePassphrase(context, recoveredKey)
                        dbBytes = decrypted.copyOfRange(4 + keyLen, decrypted.size)
                    } else {
                        dbBytes = decrypted
                    }
                } else {
                    dbBytes = decrypted
                }

                val dbPath = context.getDatabasePath("sciuro.db")

                sqlDriver.close()

                val backupFile = File("${dbPath.absolutePath}.pre_import_backup")
                if (dbPath.exists()) {
                    dbPath.copyTo(backupFile, overwrite = true)
                }

                dbPath.parentFile?.mkdirs()
                dbPath.writeBytes(dbBytes)

                File("${dbPath.absolutePath}-wal").delete()
                File("${dbPath.absolutePath}-shm").delete()
                File("${dbPath.absolutePath}-journal").delete()

                Result.success("Restored successfully — previous database backed up to ${backupFile.name}")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
