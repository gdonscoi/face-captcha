package br.com.oiti.certiface.challenge.camera2

import android.hardware.camera2.CameraCaptureSession


class CameraCaptureSessionStateCallback(private val onConfiguredCallback: (session: CameraCaptureSession) -> Unit): CameraCaptureSession.StateCallback() {

    override fun onConfigured(session: CameraCaptureSession) {
        onConfiguredCallback(session)
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
    }
}
