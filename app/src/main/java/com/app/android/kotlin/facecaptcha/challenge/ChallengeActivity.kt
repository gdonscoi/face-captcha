package com.app.android.kotlin.facecaptcha.challenge

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.app.android.kotlin.facecaptcha.R
import kotlinx.android.synthetic.main.activity_challenge.*


class ChallengeActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback, CameraContract.View {

    private var TAG = this.javaClass.name

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    private var preview: FrameLayout? = null

    private lateinit var presenter: CameraPresenter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
        presenter.destroy()
    }

    override fun onResume() {
        super.onResume()

        presenter = CameraPresenter(this@ChallengeActivity)

        if (checkCameraRequirements()) {
            // Create an instance of Camera
            mCamera = getCameraInstance()

            // Create our Preview view and set it as the content of our activity.
            mPreview = CameraPreview(this, mCamera!!)

            preview = findViewById<View>(R.id.camera_preview) as FrameLayout
            preview?.addView(mPreview)
        }

        presenter.start(MOCK_PARAMS)
    }

    override fun takePicture(pictureCallback: Camera.PictureCallback) {
        mCamera?.takePicture(null, null, pictureCallback)
    }

    override fun loadIcon(base64Image: String) {
        if (base64Image == null || base64Image.isEmpty()) {
            return
        }
        val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        runOnUiThread {
            (iconField as ImageView).setImageBitmap(decodedByte)
        }
    }

    override fun setMessage(base64message: String) {
        if (base64message == null || base64message.isEmpty()) {
            return
        }
        val decodedString = Base64.decode(base64message, Base64.DEFAULT)
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        runOnUiThread {
            (messageField as ImageView).setImageBitmap(decodedByte)
        }
    }

    override fun setCounter(count: String) {
        runOnUiThread {
            counterField.text = count
        }
    }

    override fun showView() {
        runOnUiThread {
            iconField.visibility = View.VISIBLE
            messageField.visibility = View.VISIBLE
            counterField.visibility = View.VISIBLE
        }
    }

    override fun finishCaptcha(message: String) {
        runOnUiThread {
//            messageField.text = message
        }
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
                    cam.setDisplayOrientation(90)
                } catch (e: RuntimeException) {
                    printToast("Camera failed to open", e)
                }
            }
        }

        return cam
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
    }
}
