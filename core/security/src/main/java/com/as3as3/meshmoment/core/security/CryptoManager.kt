package com.as3as3.meshmoment.core.security

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

class CryptoManager {

    private val algorithm = "AES/CBC/PKCS5Padding"
    private val keyAlgorithm = "AES"

    fun encrypt(data: String, key: String): String {
        // Ensure key is 32 bytes for AES-256
        val keyBytes = key.toByteArray().copyOf(32)
        val secretKey = SecretKeySpec(keyBytes, keyAlgorithm)
        val cipher = Cipher.getInstance(algorithm)
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        
        val combined = iv + encryptedBytes
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encryptedData: String, key: String): String {
        val combined = Base64.getDecoder().decode(encryptedData)
        val iv = combined.sliceArray(0..15)
        val encryptedBytes = combined.sliceArray(16 until combined.size)
        
        val keyBytes = key.toByteArray().copyOf(32)
        val secretKey = SecretKeySpec(keyBytes, keyAlgorithm)
        val cipher = Cipher.getInstance(algorithm)
        val ivSpec = IvParameterSpec(iv)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        return String(decryptedBytes)
    }
}