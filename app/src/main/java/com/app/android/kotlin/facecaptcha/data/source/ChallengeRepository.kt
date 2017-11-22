package com.app.android.kotlin.facecaptcha.data.source

import com.app.android.kotlin.facecaptcha.ChallengeCallback
import com.app.android.kotlin.facecaptcha.data.model.challenge.CaptchaResponse
import com.app.android.kotlin.facecaptcha.data.source.remote.ChallengeRemoteDataSource


/**
 * Created by bzumpano on 16/11/17.
 */
class ChallengeRepository(baseUrl: String, appKey: String) {

    private val dataSource: ChallengeRemoteDataSource = ChallengeRemoteDataSource(baseUrl, appKey)

    fun challenge(params: String, callback: ChallengeCallback.OnSuccess? = null) {
        Thread({
            val response = dataSource.challenge(params)
            callback?.run(response)
        }).start()
    }

    fun captcha(chkey: String, images: List<String>, callback: ChallengeCallback.OnSuccess? = null): CaptchaResponse {
        return CaptchaResponse(true, "", "", "")
    }

}
