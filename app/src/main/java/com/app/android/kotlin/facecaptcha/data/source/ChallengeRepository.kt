package com.app.android.kotlin.facecaptcha.data.source

import com.app.android.kotlin.facecaptcha.data.model.challenge.CaptchaResponse
import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse
import com.app.android.kotlin.facecaptcha.data.source.remote.ChallengeRemoteDataSource


/**
 * Created by bzumpano on 16/11/17.
 */
class ChallengeRepository(baseUrl: String, appKey: String) {

    private val dataSource: ChallengeRemoteDataSource = ChallengeRemoteDataSource(baseUrl, appKey)

    fun challenge(params: String, onSuccess: (response: ChallengeResponse) -> Unit) {
        Thread({
            val response = dataSource.challenge(params)
            onSuccess(response)
        }).start()
    }

    fun captcha(chkey: String, images: List<String>, callback: (response: ChallengeResponse) -> Unit): CaptchaResponse {
        return CaptchaResponse(true, "", "", "")
    }

}
