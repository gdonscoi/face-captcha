package com.app.android.kotlin.facecaptcha.data.source.remote

import com.app.android.kotlin.facecaptcha.data.model.challenge.CaptchaResponse
import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse
import com.app.android.kotlin.facecaptcha.data.source.remote.api.ChallengeApi
import com.app.android.kotlin.facecaptcha.data.util.CryptoData
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

        return gson.fromJson(json, ChallengeResponse::class.java)
    }

    fun captcha(chkey: String, images: List<String>): CaptchaResponse {
        return CaptchaResponse(true, "", "", "")
    }

}
