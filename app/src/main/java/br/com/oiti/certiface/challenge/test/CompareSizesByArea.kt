package br.com.oiti.certiface.challenge.test

import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Size
import java.util.Comparator

/**
 * Created by bzumpano on 25/11/17.
 */
internal class CompareSizesByArea : Comparator<Size> {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun compare(lhs: Size, rhs: Size): Int {
        // We cast here to ensure the multiplications won't overflow
        return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
    }

}
