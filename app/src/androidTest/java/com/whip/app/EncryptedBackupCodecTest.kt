package com.whip.app

import com.whip.app.data.EncryptedBackupCodec
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupCodecTest {
    @Test
    fun authenticatedBackupRoundTripsWithoutPersistingPassphrase() {
        val plaintext = """{"format":"whip-backup","tables":{"tasks":[]}}"""
        val encrypted = EncryptedBackupCodec.encrypt(plaintext, "correct horse".toCharArray())

        assertTrue(EncryptedBackupCodec.isEncrypted(encrypted))
        assertTrue("correct horse" !in encrypted)
        assertTrue(plaintext !in encrypted)
        assertEquals(plaintext, EncryptedBackupCodec.decrypt(encrypted, "correct horse".toCharArray()))
    }

    @Test
    fun wrongPassphraseAndTamperingAreRejected() {
        val encrypted = EncryptedBackupCodec.encrypt("private", "correct horse".toCharArray())
        assertTrue(runCatching { EncryptedBackupCodec.decrypt(encrypted, "wrong password".toCharArray()) }.isFailure)

        val root = JSONObject(encrypted)
        val ciphertext = root.getString("ciphertextBase64")
        root.put("ciphertextBase64", (if (ciphertext.first() == 'A') "B" else "A") + ciphertext.drop(1))
        assertTrue(runCatching { EncryptedBackupCodec.decrypt(root.toString(), "correct horse".toCharArray()) }.isFailure)
    }
}
