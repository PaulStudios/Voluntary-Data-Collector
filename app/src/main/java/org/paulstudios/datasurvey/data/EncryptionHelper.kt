package org.paulstudios.datasurvey.data

import android.util.Base64
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES"

    fun encrypt(data: String, secretKey: String): String {
        val key = generateKey(secretKey)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedData = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encryptedData, Base64.DEFAULT)
    }

    fun decrypt(data: String, secretKey: String): String {
        val key = generateKey(secretKey)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val decodedData = Base64.decode(data, Base64.DEFAULT)
        val decryptedData = cipher.doFinal(decodedData)
        return String(decryptedData)
    }

    private fun generateKey(secretKey: String): Key {
        return SecretKeySpec(secretKey.toByteArray(), ALGORITHM)
    }
}