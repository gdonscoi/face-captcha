package br.com.oiti.certiface.challenge

import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.view.Surface
import android.view.View


class ChallengeActivity : AbstractChallengeActivity() {

    private var camera: Camera? = null
    private var cameraPreview: CameraPreview? = null


    override fun getCameraPreview(): View? = cameraPreview

    override fun onResume() {
        super.onResume()

        if (hasCameraRequirements()) {
            camera = openFrontFacingCamera()

            // Create our Preview view and set it as the content of our activity.
            cameraPreview = CameraPreview(this, camera!!)

            preview.addView(cameraPreview)
        }
    }

    override fun buildTakePictureCallback(photos: HashMap<ByteArray, String>, afterTakePicture: (data: ByteArray) -> Unit): Any {
        val callback = Camera.PictureCallback { data, camera ->
            afterTakePicture(data)
            camera.startPreview()
        }

        return callback
    }


    override fun takePicture(callback: Any) {
        camera?.takePicture(null, null, callback as Camera.PictureCallback)
    }

    override fun getFrontFacingCameraId(): String? {
        var count = Camera.getNumberOfCameras()
        val info = Camera.CameraInfo()

        (0..count).forEach({ id ->
            Camera.getCameraInfo(id, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return id.toString()
            }
        })
        return null
    }


    override fun releaseCamera() {
        camera?.release()
        camera = null
    }

    private fun openFrontFacingCamera(): Camera? {
        var cam: Camera? = null

        getFrontFacingCameraId()?.let { camId ->
            Camera.open(camId.toInt()).let {
                cam = it
                setCameraDisplayOrientation(this@ChallengeActivity, camId.toInt(), it)
                setCameraParameters(it)
            }
        }

        return cam
    }

    private fun setCameraParameters(camera: Camera) {
        val parameters = camera.parameters
        val lowerSupportedPictureSize = parameters.supportedPictureSizes.last()

        parameters.pictureFormat = ImageFormat.JPEG
        parameters.setPictureSize(lowerSupportedPictureSize.width, lowerSupportedPictureSize.height)

        camera.parameters = parameters
    }

    /**
     * @see Camera.setDisplayOrientation() comments
     */
    private fun setCameraDisplayOrientation(activity: Activity, cameraId: Int, camera: android.hardware.Camera) {

        val rotation = activity.windowManager.defaultDisplay.rotation

        var degrees = 0

        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)

        var result = (info.orientation + degrees) % 360
        result = (360 - result) % 360  // compensate the mirror

        camera.setDisplayOrientation(result)
    }
}
