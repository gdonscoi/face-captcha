package br.com.oiti.certiface.data.challenge

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast


class ChallengeActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback, CameraContract.View {

    private var TAG = this.javaClass.name

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    private var preview: FrameLayout? = null

    private lateinit var presenter: CameraPresenter

    private lateinit var userParams: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        userParams = intent.getStringExtra(USER_INFO_KEY)

        button_start.setOnClickListener { presenter.start(userParams) }
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
        presenter.destroy()
    }

    override fun onResume() {
        super.onResume()

        presenter = CameraPresenter(this@ChallengeActivity)
        initialView()

        if (checkCameraRequirements()) {
            // Create an instance of Camera
            mCamera = getCameraInstance()

            // Create our Preview view and set it as the content of our activity.
            mPreview = CameraPreview(this, mCamera!!)

            preview = findViewById<View>(R.id.camera_preview) as FrameLayout
            preview?.addView(mPreview)
        }
    }

    override fun initialView() {
        runOnUiThread {
            initialContainer.visibility = View.VISIBLE
            visibilityAnimationFeedback(View.GONE, "")
            iconField.setImageBitmap(null)
            messageField.setImageBitmap(null)
            counterField.text = ""
        }
    }

    override fun startChallenge() {
        runOnUiThread {
            initialContainer.visibility = View.GONE
            visibilityChallengeContainer(View.VISIBLE)
            loadingContainer.visibility = View.GONE
            feedbackAnimationContainer.visibility = View.GONE
        }
    }

    override fun takePicture(callback: Camera.PictureCallback) {
        mCamera?.takePicture(null, null, callback)
    }

    override fun loadIcon(icon: Bitmap?) {
        runOnUiThread {
            iconField.setImageBitmap(icon)
        }
    }

    override fun setMessage(message: Bitmap?) {
        runOnUiThread {
            messageField.setImageBitmap(message)
        }
    }

    override fun setCounter(count: String) {
        runOnUiThread {
            counterField.text = count
        }
    }

    override fun loadingView() {
        runOnUiThread {
            visibilityChallengeContainer(View.GONE)
            loadingContainer.visibility = View.VISIBLE
        }
    }

    override fun finishChallenge(valid: Boolean) {
        val data = Intent()

        data.putExtra(ACTIVITY_RESULT_KEY, valid)
        setResult(RESULT_OK, data)

        finish()
    }

    override fun animationFeedback(visibility: Int, message: String) {
        runOnUiThread {
            visibilityChallengeContainer(View.GONE)
            loadingContainer.visibility = View.GONE
            visibilityAnimationFeedback(visibility, message)
        }
    }

    private fun visibilityAnimationFeedback(visibility: Int, message: String) {
        feedbackAnimationContainer.visibility = visibility
        resultContainer.visibility = visibility
        textAnimation.text = message
        textResult.text = message
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    private fun visibilityChallengeContainer(visibility: Int) {
        challengeContainer.visibility = visibility
        iconField.visibility = visibility
    }

    private fun releaseCamera() {
        mCamera?.release()
        mCamera = null

        preview?.removeView(mPreview)
    }

    private fun checkCameraRequirements(): Boolean {


        if (!checkFrontalCameraHardware(applicationContext)) {
            Log.d(TAG, "Frontal camera not detected. ")
            return false
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
            return false
        }

        return true
    }

    /** Check if this device has a frontal camera  */
    private fun checkFrontalCameraHardware(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

    /**
     *
     * A safe way to get an instance of the Camera object.
     *
     * @return null if camera is unavailable
     */
    private fun getCameraInstance(): Camera? {
        var c: Camera? = null
        try {
            c = openFrontFacingCamera() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
        }

        return c
    }

    private fun openFrontFacingCamera(): Camera? {
        var cameraCount = 0
        var cam: Camera? = null
        val cameraInfo = Camera.CameraInfo()
        cameraCount = Camera.getNumberOfCameras()
        for (camIdx in 0 until cameraCount) {
            Camera.getCameraInfo(camIdx, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx)

                    setCameraDisplayOrientation(this@ChallengeActivity, cameraInfo, cam)
                    setCameraParameters(cam!!)
                } catch (e: RuntimeException) {
                    printToast("Camera failed to open", e)
                }
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
    private fun setCameraDisplayOrientation(activity: Activity, info: Camera.CameraInfo, camera: android.hardware.Camera) {

        val rotation = activity.windowManager.defaultDisplay.rotation

        var degrees = 0

        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }

    private fun printToast(text: String, e: Throwable? = null) {
        val context = applicationContext
        val duration = Toast.LENGTH_SHORT

        val toast = Toast.makeText(context, text, duration)
        toast.show()

        if (e != null) {
            Log.d(TAG, text + ": " + e.message, e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                ErrorDialog.newInstance(getString(R.string.request_permission))
//                        .show(getChildFragmentManager(), FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {

        val REQUEST_CAMERA_PERMISSION = 1

        // Mock
        val MOCK_PARAMS = "user,comercial.token,cpf,8136822824,nome,ALESSANDRO DE OLIVEIRA FARIA,nascimento,27/05/1972"
        val USER_INFO_KEY = "user_info"
        val ACTIVITY_RESULT_KEY = "result"
    }
}
