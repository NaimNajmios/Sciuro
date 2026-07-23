package com.najmi.sciuro.export

import android.content.Context
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EncryptedExporter {

    private const val MAGIC = "SCIB"
    private const val VERSION = 1
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE = 12
    private const val SALT_SIZE = 32
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_SIZE = 256

    private val secureRandom = SecureRandom()

    suspend fun export(context: Context, passphrase: String, outputStream: OutputStream): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val dbPath = context.getDatabasePath("sciuro.db")
                if (!dbPath.exists()) return@withContext Result.failure(Exception("Database file not found"))
                val dbBytes = dbPath.readBytes()
                if (dbBytes.isEmpty()) return@withContext Result.failure(Exception("Database is empty"))

                val salt = ByteArray(SALT_SIZE)
                secureRandom.nextBytes(salt)

                val iv = ByteArray(IV_SIZE)
                secureRandom.nextBytes(iv)

                val key = deriveKey(passphrase, salt)
                val secretKey = SecretKeySpec(key, "AES")
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

                val ciphertext = cipher.doFinal(dbBytes)

                val headerJson = buildString {
                    append("{")
                    append("\"magic\":\"$MAGIC\",")
                    append("\"version\":$VERSION,")
                    append("\"createdAt\":${System.currentTimeMillis()},")
                    append("\"salt\":\"${Base64.encodeToString(salt, Base64.NO_WRAP)}\",")
                    append("\"iv\":\"${Base64.encodeToString(iv, Base64.NO_WRAP)}\"")
                    append("}")
                }

                val headerBytes = headerJson.toByteArray(Charsets.UTF_8)
                val headerLenBytes = ByteArray(4)
                headerLenBytes[0] = ((headerBytes.size shr 24) and 0xFF).toByte()
                headerLenBytes[1] = ((headerBytes.size shr 16) and 0xFF).toByte()
                headerLenBytes[2] = ((headerBytes.size shr 8) and 0xFF).toByte()
                headerLenBytes[3] = (headerBytes.size and 0xFF).toByte()

                val output = ByteArrayOutputStream()
                output.write(headerLenBytes)
                output.write(headerBytes)
                output.write(ciphertext)
                outputStream.write(output.toByteArray())
                outputStream.flush()

                Result.success(dbPath.name)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
