package com.app.android.kotlin.facecaptcha.challenge

import android.hardware.Camera

interface CameraContract {
    interface View {

        fun takePicture(pictureCallback: Camera.PictureCallback)

        fun loadIcon(iconBinary: String)

        fun setMessage(message: String)

        fun setCounter(count: String)

        fun showView()

        fun finishCaptcha(message: String)
    }
}
