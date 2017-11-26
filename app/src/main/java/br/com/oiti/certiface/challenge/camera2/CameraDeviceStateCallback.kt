package br.com.oiti.certiface.challenge.camera2

import android.hardware.camera2.CameraDevice
import android.os.Build
import android.support.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CameraDeviceStateCallback(
        private val onOpenedCallback: (camera: CameraDevice?) -> Unit,
        private val onDisconnectedCallback: (camera: CameraDevice) -> Unit,
        private val onErrorCallback: (camera: CameraDevice, error: Int) -> Unit): CameraDevice.StateCallback() {

    override fun onOpened(camera: CameraDevice?) {
        onOpenedCallback(camera)
    }

    override fun onDisconnected(camera: CameraDevice) {
        onDisconnectedCallback(camera)
    }


    override fun onError(camera: CameraDevice, error: Int) {
        onErrorCallback(camera, error)
    }
}
