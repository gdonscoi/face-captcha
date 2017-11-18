package com.app.android.kotlin.facecaptcha.challenge

interface CameraContract {
    interface View {

        fun tookPicture()

        fun loadIcon(iconBinary: String)

        fun setMessage(message: String)

        fun setCounter(count: String)

        fun showView()

        fun finishCaptcha(message: String)
    }
}
