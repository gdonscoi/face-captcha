package com.app.android.kotlin.facecaptcha.data.util

import android.os.Build
import android.support.annotation.RequiresApi
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Base64
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Created by bzumpano on 19/11/17.
 */
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
    private val skeySpec: SecretKeySpec = SecretKeySpec(key.toByteArray(), "AES")
    private val ivSpec: IvParameterSpec = IvParameterSpec(iv.toByteArray())


    @Throws(Exception::class)
    fun encrypt(data: String): String {
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec)

        val filled = padData(data)
        val enc = cipher.doFinal(filled.toByteArray())
        return String(Base64.encode(enc))
    }

    @Throws(Exception::class)
    fun decrypt(data: String): String {
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec)

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
