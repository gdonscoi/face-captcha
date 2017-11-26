package br.com.oiti.certiface.challenge.test

import android.os.Build
import android.support.annotation.RequiresApi

/**
 * Created by bzumpano on 25/11/17.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class RefCountedAutoCloseable<out T: AutoCloseable>(private var mObject: T? = null): AutoCloseable {
    private var mRefCount: Long = 0

    /**
     * Increment the reference count and return the wrapped object.
     *
     * @return the wrapped object, or null if the object has been released.
     */
    @Synchronized
    fun getAndRetain(): T? {
        if (mRefCount < 0) {
            return null
        }
        mRefCount++
        return mObject
    }

    /**
     * Return the wrapped object.
     *
     * @return the wrapped object, or null if the object has been released.
     */
    @Synchronized
    fun get(): T? {
        return mObject
    }

    /**
     * Decrement the reference count and release the wrapped object if there are no other
     * users retaining this object.
     */
    @Synchronized override fun close() {
        if (mRefCount >= 0) {
            mRefCount--
            if (mRefCount < 0) {
                try {
                    mObject!!.close()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                } finally {
                    mObject = null
                }
            }
        }
    }

}
