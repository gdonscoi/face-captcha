package br.com.oiti.certiface.challenge.camera2

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult

/**
 * Created by bzumpano on 26/11/17.
 */
class CameraCaptureSessionStateCallback(private val onConfiguredCallback: (session: CameraCaptureSession) -> Unit): CameraCaptureSession.StateCallback() {

    override fun onConfigured(session: CameraCaptureSession) {
        onConfiguredCallback(session)
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
    }
}
