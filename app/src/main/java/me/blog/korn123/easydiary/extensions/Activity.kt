package me.blog.korn123.easydiary.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import me.blog.korn123.commons.constants.Constants
import me.blog.korn123.commons.utils.CommonUtils
import me.blog.korn123.easydiary.activities.DiaryLockActivity

/**
 * Created by CHO HANJOONG on 2018-02-10.
 */

fun Activity.pauseLock() {
    val enableLock = CommonUtils.loadBooleanPreference(this, Constants.APP_LOCK_ENABLE)
    if (enableLock) {
        val currentMillis = System.currentTimeMillis()
        CommonUtils.saveLongPreference(this, Constants.SETTING_PAUSE_MILLIS, currentMillis)
    }
}

fun Activity.resumeLock() {
    val enableLock = CommonUtils.loadBooleanPreference(this, Constants.APP_LOCK_ENABLE)
    val pauseMillis = CommonUtils.loadLongPreference(this, Constants.SETTING_PAUSE_MILLIS, 0)
    if (enableLock && pauseMillis != 0L) {
        if (System.currentTimeMillis() - pauseMillis > 1000) {
            val lockDiaryIntent = Intent(this, DiaryLockActivity::class.java)
            startActivity(lockDiaryIntent)
        }
    }
}

fun Activity.openGooglePlayBy(targetAppId: String) {
    val uri = Uri.parse("market://details?id=" + targetAppId)
    val goToMarket = Intent(Intent.ACTION_VIEW, uri)
    // To count with Play market backstack, After pressing back button,
    // to taken back to our application, we need to add following flags to intent.
    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    try {
        startActivity(goToMarket)
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=" + targetAppId)))
    }
}