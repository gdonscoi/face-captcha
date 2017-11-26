package br.com.oiti.certiface.challenge.camera2

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult

/**
 * Created by bzumpano on 26/11/17.
 */
class CameraCaptureSessionCaptureCallback(private val onCaptureCompleteCallback: () -> Unit): CameraCaptureSession.CaptureCallback() {

    override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
        super.onCaptureCompleted(session, request, result)
        onCaptureCompleteCallback()
    }
}
