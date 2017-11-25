package br.com.oiti.certiface.data.challenge

import android.graphics.Bitmap
import android.hardware.Camera

interface CameraContract {
    interface View {

        fun initialView()

        fun startChallenge()

        fun takePicture(pictureCallback: Camera.PictureCallback)

        fun loadIcon(icon: Bitmap?)

        fun setMessage(message: Bitmap?)

        fun setCounter(count: String)

        fun loadingView()

        fun animationFeedback(visibility: Int, message: String)

        fun finishChallenge(valid: Boolean)
    }
}
