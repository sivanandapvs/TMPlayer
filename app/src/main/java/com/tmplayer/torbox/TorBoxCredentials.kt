package com.tmplayer.torbox

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** API 23 Keystore. Ciphertext lives outside Android backup; failures never fall back to plaintext. */
class TorBoxCredentials(context: Context) {
    private val directory = context.noBackupFilesDir
    private val file = AtomicFile(File(directory, "torbox-credential"))

    fun read(): String? = synchronized(LOCK) {
        if (!file.baseFile.exists()) return@synchronized null
        val bytes = file.openRead().use { it.readNBytesCompat(2048) }
        if (bytes.size !in 29..2048) throw TorBoxFailure("storage")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
            .takeIf(TorBoxApi::validKey) ?: throw TorBoxFailure("storage")
    }

    fun write(key: String): Unit = synchronized(LOCK) {
        if (!TorBoxApi.validKey(key)) throw TorBoxFailure("key")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        val bytes = cipher.iv + cipher.doFinal(key.toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) }
        catch (failure: Exception) { file.failWrite(output); throw TorBoxFailure("storage") }
    }

    fun clear(): Unit = synchronized(LOCK) {
        TorBoxSource.entries.forEach { source -> runCatching { libraryFile(source).delete() } }
        file.delete()
        if (file.baseFile.exists()) throw TorBoxFailure("storage")
        // Once ciphertext is removed the account is disconnected, even if the Keystore is unavailable.
        runCatching {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
        }
        Unit
    }

    fun readLibrary(key: String, source: TorBoxSource): TorBoxPage? = synchronized(LOCK) {
        val snapshot = libraryFile(source)
        if (!snapshot.baseFile.exists()) return@synchronized null
        try {
            val bytes = snapshot.openRead().use { it.readNBytesCompat(MAX_CACHE_BYTES + 28) }
            if (bytes.size !in 29..MAX_CACHE_BYTES + 28) return@synchronized null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            val text = String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
            TorBoxApi.cachedPage(text, owner(key), source)
        } catch (_: Exception) { null }
    }

    fun writeLibrary(key: String, source: TorBoxSource, page: TorBoxPage): Boolean = synchronized(LOCK) {
        try {
            val text = TorBoxApi.cachePage(page, owner(key), source).toByteArray(Charsets.UTF_8)
            if (text.size > MAX_CACHE_BYTES) return@synchronized false
            if (directory.usableSpace < text.size.toLong() + 28 + 64 * 1024) return@synchronized false
            val snapshot = libraryFile(source)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, masterKey())
            val bytes = cipher.iv + cipher.doFinal(text)
            val output = snapshot.startWrite()
            try { output.write(bytes); snapshot.finishWrite(output) }
            catch (failure: Exception) { snapshot.failWrite(output); throw failure }
            true
        } catch (_: Exception) { false }
    }

    private fun libraryFile(source: TorBoxSource) = AtomicFile(File(directory, "torbox-library-${source.path}"))

    private fun owner(key: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(key.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun masterKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (output.size() <= limit) {
            val count = read(buffer, 0, minOf(buffer.size, limit + 1 - output.size()))
            if (count < 0) break
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    companion object {
        private const val ALIAS = "veylora.torbox.v1"
        private val LOCK = Any()
        private const val MAX_CACHE_BYTES = 8 * 1024 * 1024
    }
}
