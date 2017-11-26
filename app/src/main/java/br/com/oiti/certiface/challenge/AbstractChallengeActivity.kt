package br.com.oiti.certiface.challenge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import br.com.oiti.certiface.R
import kotlinx.android.synthetic.main.activity_challenge.*
import kotlinx.android.synthetic.main.challenge_view.*
import kotlinx.android.synthetic.main.feedback_animation.*
import kotlinx.android.synthetic.main.initial_view.*
import kotlinx.android.synthetic.main.loading_view.*
import kotlinx.android.synthetic.main.result_view.*


abstract class AbstractChallengeActivity: AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback, CameraContract.View {

    private lateinit var presenter: CameraPresenter

    private val endpoint by lazy { intent.getStringExtra(PARAM_ENDPOINT) }
    private val appKey by lazy { intent.getStringExtra(PARAM_APP_KEY) }
    private val userParams by lazy { intent.getStringExtra(PARAM_USER_INFO) }

    abstract fun getFrontFacingCameraId(): Int?

    abstract fun releaseCamera()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        button_start.setOnClickListener { presenter.start(userParams) }
    }

    override fun onResume() {
        super.onResume()

        presenter = CameraPresenter(this@AbstractChallengeActivity, endpoint, appKey)
        initialView()
    }

    override fun onPause() {
        super.onPause()
        presenter.destroy()
        releaseCamera()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                printToast("Sorry!!!, you can't use this app without granting permission")
                finish()
            }
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

        data.putExtra(PARAM_ACTIVITY_RESULT, valid)
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


    protected fun hasCameraRequirements(): Boolean {


        if (!checkFrontalCameraHardware(applicationContext)) {
            Log.d(TAG, "Frontal camera not detected. ")
            return false
        }

        return hasCameraPermissions()
    }

    protected fun printToast(text: String, e: Throwable? = null) {
        val context = applicationContext
        val duration = Toast.LENGTH_SHORT

        val toast = Toast.makeText(context, text, duration)
        toast.show()

        e?.let { Log.d(TAG, text + ": " + e.message, e) }
    }



    private fun visibilityAnimationFeedback(visibility: Int, message: String) {
        feedbackAnimationContainer.visibility = visibility
        resultContainer.visibility = visibility
        textAnimation.text = message
        textResult.text = message
    }

    private fun visibilityChallengeContainer(visibility: Int) {
        challengeContainer.visibility = visibility
        iconField.visibility = visibility
    }

    /** Check if this device has a frontal camera  */
    private fun checkFrontalCameraHardware(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

    private fun hasCameraPermissions(): Boolean {
        // Add permission for camera and let user grant the permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return false
        }

        return true
    }


    companion object {

        val PARAM_APP_KEY = "app_key"
        val PARAM_ENDPOINT = "endpoint"
        val PARAM_USER_INFO = "user_info"
        val PARAM_ACTIVITY_RESULT = "certiface_result"

        @JvmStatic
        protected val TAG = this::class.java.name

        @JvmStatic
        protected val REQUEST_CAMERA_PERMISSION = 200
    }

}
