package com.app.android.kotlin.facecaptcha

import com.app.android.kotlin.facecaptcha.data.model.challenge.ChallengeResponse

/**
 * Created by bzumpano on 21/11/17.
 */
interface ChallengeCallback {
    interface OnBefore: Callback<Unit>
    interface OnSuccess: Callback<ChallengeResponse>
}
