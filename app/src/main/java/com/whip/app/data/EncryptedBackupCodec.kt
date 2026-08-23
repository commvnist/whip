package com.whip.app.data

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/** Passphrase encryption for user-movable backups. The passphrase is never stored. */
object EncryptedBackupCodec {
    private const val FORMAT = "whip-encrypted-backup"
    private const val VERSION = 1
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val MINIMUM_PASSPHRASE_LENGTH = 8

    fun isEncrypted(value: String): Boolean = runCatching {
        val root = JSONObject(value)
        root.optString("format") == FORMAT
    }.getOrDefault(false)

    fun encrypt(plainBackup: String, passphrase: CharArray, random: SecureRandom = SecureRandom()): String {
        require(passphrase.size >= MINIMUM_PASSPHRASE_LENGTH) {
            "Use at least $MINIMUM_PASSPHRASE_LENGTH characters for the backup passphrase"
        }
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val key = deriveKey(passphrase, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad(VERSION, ITERATIONS))
        val ciphertext = cipher.doFinal(plainBackup.toByteArray(Charsets.UTF_8))
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("cipher", "AES-256-GCM")
            .put("kdf", "PBKDF2-HMAC-SHA256")
            .put("iterations", ITERATIONS)
            .put("saltBase64", Base64.getEncoder().encodeToString(salt))
            .put("nonceBase64", Base64.getEncoder().encodeToString(nonce))
            .put("ciphertextBase64", Base64.getEncoder().encodeToString(ciphertext))
            .toString(2)
    }

    fun decrypt(encryptedBackup: String, passphrase: CharArray): String {
        val root = runCatching { JSONObject(encryptedBackup) }
            .getOrElse { error("This is not a valid encrypted Whip backup") }
        require(root.optString("format") == FORMAT) { "This is not an encrypted Whip backup" }
        val version = root.optInt("version")
        require(version == VERSION) { "Unsupported encrypted backup version $version" }
        require(root.optString("cipher") == "AES-256-GCM") { "Unsupported backup cipher" }
        require(root.optString("kdf") == "PBKDF2-HMAC-SHA256") { "Unsupported backup key derivation" }
        val iterations = root.optInt("iterations")
        require(iterations in 100_000..2_000_000) { "Invalid backup key-derivation cost" }
        return try {
            val decoder = Base64.getDecoder()
            val salt = decoder.decode(root.getString("saltBase64"))
            val nonce = decoder.decode(root.getString("nonceBase64"))
            val ciphertext = decoder.decode(root.getString("ciphertextBase64"))
            require(salt.size >= 16 && nonce.size == 12 && ciphertext.size >= 16) {
                "Encrypted backup parameters are invalid"
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad(version, iterations))
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        } catch (error: Throwable) {
            throw IllegalArgumentException("Wrong passphrase or encrypted backup was modified", error)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun aad(version: Int, iterations: Int) = "$FORMAT|$version|$iterations".toByteArray(Charsets.UTF_8)
}
