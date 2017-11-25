package br.com.oiti.certiface.data.util

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Base64
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class CryptoData(appKey: String) {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    companion object {
        val ALGORITHM = "AES/CBC/PKCS7Padding"

        val BLOCK_SIZE = 16
    }

    private val key = appKey.substring(0, 16)
    private val iv = appKey.reversed().substring(0, 16)

    private val cipher: Cipher = Cipher.getInstance(ALGORITHM)
    private val secretKeySpec: SecretKeySpec = SecretKeySpec(key.toByteArray(), "AES")
    private val ivSpec: IvParameterSpec = IvParameterSpec(iv.toByteArray())


    fun encrypt(data: String): String {
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)

        val filled = padData(data)
        val enc = cipher.doFinal(filled.toByteArray())
        return String(Base64.encode(enc))
    }

    fun decrypt(data: String): String {
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)

        val decString = Base64.decode(data.toByteArray())

        val dec = cipher.doFinal(decString)
        return String(dec).trim()
    }


    private fun padData(source: String): String {
        val remainder = source.length % BLOCK_SIZE
        val padLength = BLOCK_SIZE - remainder
        val size = source.length + padLength

        return source.padEnd(size)
    }

}
