package com.app.android.kotlin.facecaptcha.challenge

import android.graphics.Bitmap
import android.hardware.Camera

interface CameraContract {
    interface View {

        fun takePicture(pictureCallback: Camera.PictureCallback)

        fun loadIcon(icon: Bitmap?)

        fun setMessage(message: Bitmap?)

        fun setCounter(count: String)

        fun showView()

        fun finishCaptcha(message: String)
    }
}
