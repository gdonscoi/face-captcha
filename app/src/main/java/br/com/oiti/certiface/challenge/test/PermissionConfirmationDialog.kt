package br.com.oiti.certiface.challenge.test

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v13.app.ActivityCompat
import android.support.v4.app.DialogFragment
import br.com.oiti.certiface.R


/**
 * A dialog that explains about the necessary permissions.
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class PermissionConfirmationDialog : DialogFragment() {

    /**
     * Request code for camera permissions.
     */
    private val REQUEST_CAMERA_PERMISSIONS = 1

    /**
     * Permissions required to take a picture.
     */
    private val CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val parent = parentFragment.activity
        return AlertDialog.Builder(activity)
                .setMessage(R.string.request_permission)
                .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                    ActivityCompat.requestPermissions(parent, CAMERA_PERMISSIONS,
                            REQUEST_CAMERA_PERMISSIONS)
                })
                .setNegativeButton(android.R.string.cancel,
                        DialogInterface.OnClickListener { dialog, which -> activity.finish() })
                .create()
    }

    companion object {

        fun newInstance(): PermissionConfirmationDialog {
            return PermissionConfirmationDialog()
        }
    }

}
