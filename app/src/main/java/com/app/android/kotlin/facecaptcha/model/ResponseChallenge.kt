package com.app.android.kotlin.facecaptcha.model

import com.google.gson.annotations.SerializedName

class ResponseChallenge {

    @SerializedName("chkey")
    var key: String = ""

    var totalTime: Int = 0

    @SerializedName("snapFrequenceInMillis")
    var snapTime: Int = 0

    var challenges: MutableCollection<Challenge> = ArrayList()
}