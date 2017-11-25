package br.com.oiti.certiface.data.source.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Base64
import android.util.Log
import br.com.oiti.certiface.data.model.challenge.CaptchaResponse
import br.com.oiti.certiface.data.source.remote.api.ChallengeApi
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream


class ChallengeRemoteDataSource(baseUrl: String, private val appKey: String) {

    private val api: ChallengeApi
    private val gson = Gson()
    private val cryptoData = CryptoData(appKey)

    init {
        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        api = retrofit.create(ChallengeApi::class.java)
    }

    fun challenge(params: String): ChallengeResponse {

        val call = api.challenge(appKey, cryptoData.encrypt(params))
        val response = call.execute()
        val encrypted = response.body()
        val json = cryptoData.decrypt(encrypted!!)

        val challengeResponse = gson.fromJson(json, ChallengeResponse::class.java)

        challengeResponse.challenges.map {
            val decodedMessage = Base64.decode(it.mensagem, Base64.DEFAULT)
            it.decodedMessage = BitmapFactory.decodeByteArray(decodedMessage, 0, decodedMessage.size)

            val decodedIcon = Base64.decode(it.tipoFace.imagem, Base64.DEFAULT)
            it.decodedIcon = BitmapFactory.decodeByteArray(decodedIcon, 0, decodedIcon.size)
        }

        return challengeResponse
    }

    /**
     *
     * images: Map<key, value> = key: image, value: tipoFace.codigo
     */
    fun captcha(chkey: String, images: Map<ByteArray, String>): CaptchaResponse {
        val stringImages = imagesToString(images)
        val encryptedImages = cryptoData.encrypt(stringImages)

        val call = api.captcha(appKey, chkey, encryptedImages)
        val response = call.execute()
        val captchaResponse = response.body()

        return captchaResponse!!
    }

    private fun imagesToString(images: Map<ByteArray, String>): String {

        val stringImages = ArrayList<String>()

        for((key, value) in images) {
            createImage(key)
            val imageBase64 = Base64.encodeToString(key, Base64.NO_WRAP)

            stringImages.add("data:image/jpeg;base64,type:$value,$imageBase64")
        }

        return stringImages.joinToString(separator = "")
    }

    private fun createImage(byteImage: ByteArray) {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + "/Facecaptcha")
        val timeMillis = System.currentTimeMillis()
        val photo = File(dir, "photo_$timeMillis.jpg")

        dir.mkdirs()

        if (photo.exists()) {
            photo.delete()
        }

        try {
            Log.d(this::class.java.name, "Image created at " + photo.path)
            val fos = FileOutputStream(photo.path)

            fos.write(byteImage)
            fos.close()
        } catch (e: java.io.IOException) {
            Log.e(this::class.java.name, "Exception in photoCallback", e)
        }
        Bitmap.Config.RGB_565
    }

}
