package br.com.oiti.certiface.challenge

import android.app.Activity
import android.hardware.Camera
import android.view.View
import br.com.oiti.certiface.challenge.camera.CameraPreview
import kotlinx.android.synthetic.main.challenge_fragment.*


/**
 * Uses old camera api
 */
class CameraFragment : AbstractChallengeFragment() {

    private var camera: Camera? = null
    private var cameraPreview: CameraPreview? = null


    override fun getCameraPreview(): View? = cameraPreview


    override fun onResume() {
        super.onResume()

        if (hasCameraRequirements()) {
            camera = openFrontFacingCamera()

            // Create our Preview view and set it as the content of our activity.
            cameraPreview = CameraPreview(activity, camera!!)

            cameraFrameLayout.addView(cameraPreview)
        }
    }

    override fun buildTakePictureHandler(
            photos: HashMap<ByteArray, String>,
            afterTakePicture: (data: ByteArray) -> Unit): Any = Camera.PictureCallback { data, camera ->
        afterTakePicture(data)
        camera.startPreview()
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
                setCameraDisplayOrientation(activity, it)
                setCameraParameters(it)
            }
        }

        return cam
    }

    private fun setCameraParameters(camera: Camera) {
        val parameters = camera.parameters
        val size = getBestSupportedImageSize(camera)

        parameters.pictureFormat = IMAGE_FORMAT
        parameters.setPictureSize(size.width, size.height)

        camera.parameters = parameters
    }

    /**
     * @see Camera.setDisplayOrientation() comments
     */
    private fun setCameraDisplayOrientation(activity: Activity, camera: android.hardware.Camera) {
        val rotation = getRotation(activity)!!

        // set image orientation
        val params = camera.parameters
        params.setRotation(rotation)
        camera.parameters = params


        // set preview orientation
        val result = (360 - rotation) % 360  // compensate the mirror
        camera.setDisplayOrientation(result)
    }

    private fun getBestSupportedImageSize(camera: Camera): Camera.Size {
        val parameters = camera.parameters
        val pixelsCount = IMAGE_WIDTH * IMAGE_HEIGHT

        parameters.supportedPictureSizes.forEach { size ->
            val currentPixelsCount = size.width * size.height

            if (currentPixelsCount < pixelsCount) {
                return size
            }
        }

        return camera.Size(IMAGE_WIDTH, IMAGE_HEIGHT)
    }
}
