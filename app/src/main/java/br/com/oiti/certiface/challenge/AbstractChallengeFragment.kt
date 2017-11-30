package br.com.oiti.certiface.challenge

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import br.com.oiti.certiface.R
import br.com.oiti.certiface.data.model.challenge.CaptchaResponse
import kotlinx.android.synthetic.main.challenge_fragment.*
import kotlinx.android.synthetic.main.challenge_view.*
import kotlinx.android.synthetic.main.feedback_animation.*
import kotlinx.android.synthetic.main.initial_view.*
import kotlinx.android.synthetic.main.loading_view.*
import kotlinx.android.synthetic.main.result_view.*


abstract class AbstractChallengeFragment: Fragment(), ChallengeContract.View {

    protected var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    private lateinit var endpoint: String
    private lateinit var appKey: String
    private lateinit var userParams: String

    private var presenter: ChallengePresenter? = null

    abstract fun getFrontFacingCameraId(): String?
    abstract fun releaseCamera()
    abstract fun getCameraPreview(): View?

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        endpoint = arguments.getString(ChallengeActivity.PARAM_ENDPOINT)
        appKey = arguments.getString(ChallengeActivity.PARAM_APP_KEY)
        userParams = arguments.getString(ChallengeActivity.PARAM_USER_INFO)

        return inflater!!.inflate(R.layout.challenge_fragment, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonStart.setOnClickListener { presenter!!.start(userParams) }
    }

    override fun onResume() {
        super.onResume()

        startBackgroundThread()

        presenter = ChallengePresenter(backgroundHandler!!, this@AbstractChallengeFragment, endpoint, appKey)
        initialView()
    }

    override fun onPause() {
        super.onPause()

        Thread({
            stopBackgroundThread()
            releaseCamera()
        }).start()

        cameraFrameLayout.removeView(getCameraPreview())

        presenter = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                printToast("Sorry!!!, you can't use this app without granting permission")
                activity.finish()
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

    override fun animationFeedback(visibility: Int, message: String) {
        runOnUiThread {
            visibilityChallengeContainer(View.GONE)
            loadingContainer.visibility = View.GONE
            visibilityAnimationFeedback(visibility, message)
        }
    }

    override fun finishChallenge(response: CaptchaResponse) {
        val data = Intent()

        data.putExtra(ChallengeActivity.PARAM_ACTIVITY_RESULT, response.valid)
        data.putExtra(ChallengeActivity.PARAM_ACTIVITY_RESULT_HASH, response.hash)
        data.putExtra(ChallengeActivity.PARAM_ACTIVITY_RESULT_PROTOCOL, response.protocol)

        activity.setResult(AppCompatActivity.RESULT_OK, data)
        activity.finish()
    }

    private fun stopBackgroundThread() {
//        backgroundHandler?.removeCallbacksAndMessages(null)

        backgroundHandler?.looper?.thread?.interrupt()
        backgroundThread?.looper?.thread?.interrupt()
        backgroundThread?.interrupt()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            backgroundThread?.quitSafely()
        } else {
            backgroundThread?.quit()
        }

        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread(this::class.java.simpleName)
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun runOnUiThread(action: () -> Unit) {
        activity?.runOnUiThread(action)
    }

    private fun visibilityChallengeContainer(visibility: Int) {
        challengeContainer.visibility = visibility
        iconField.visibility = visibility
    }

    private fun visibilityAnimationFeedback(visibility: Int, message: String) {
        feedbackAnimationContainer.visibility = visibility
        resultContainer.visibility = visibility
        textAnimation.text = message
        textResult.text = message
    }

    protected fun hasCameraRequirements(): Boolean {

        if (!checkFrontalCameraHardware(activity)) {
            Log.d(TAG, "Frontal camera not detected. ")
            return false
        }

        return hasCameraPermissions()
    }

    protected fun getRotation(activity: Activity): Int? {
        val rotation = activity.windowManager.defaultDisplay.rotation

        // Como usamos a camera frontal, os valores são invertidos
        return when (rotation) {
            Surface.ROTATION_0 -> 270
            Surface.ROTATION_90 -> 180
            Surface.ROTATION_180 -> 90
            Surface.ROTATION_270 -> 0
            else -> null
        }
    }

    private fun printToast(text: String, e: Throwable? = null) {
        val context = activity
        val duration = Toast.LENGTH_SHORT

        val toast = Toast.makeText(context, text, duration)
        toast.show()

        e?.let { Log.d(TAG, text + ": " + e.message, e) }
    }

    /** Check if this device has a frontal camera  */
    private fun checkFrontalCameraHardware(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

    private fun hasCameraPermissions(): Boolean {
        // Add permission for camera and let user grant the permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return false
        }

        return true
    }


    companion object {
        @JvmStatic
        protected val IMAGE_FORMAT = ImageFormat.JPEG

        @JvmStatic
        protected val IMAGE_WIDTH = 320

        @JvmStatic
        protected val IMAGE_HEIGHT = 480

        @JvmStatic
        protected val TAG = this::class.java.simpleName!!

        @JvmStatic
        protected val REQUEST_CAMERA_PERMISSION = 200
    }
}
