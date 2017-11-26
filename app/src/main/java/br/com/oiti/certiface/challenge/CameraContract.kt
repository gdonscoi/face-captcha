package br.com.oiti.certiface.challenge

import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Handler


interface CameraContract {
    interface View {

        fun initialView()

        fun startChallenge()

        fun buildTakePictureCallback(photos: HashMap<ByteArray, String>, afterTakePicture: (data: ByteArray) -> Unit): Any

        fun takePicture(callback: Any)

        fun loadIcon(icon: Bitmap?)

        fun setMessage(message: Bitmap?)

        fun setCounter(count: String)

        fun loadingView()

        fun animationFeedback(visibility: Int, message: String)

        fun finishChallenge(valid: Boolean)
    }
}
