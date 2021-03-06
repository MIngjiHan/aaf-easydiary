package me.blog.korn123.easydiary.activities

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.isOreoPlus
import io.github.aafactory.commons.utils.DateUtils
import kotlinx.android.synthetic.main.activity_dev.*
import me.blog.korn123.commons.utils.EasyDiaryUtils
import me.blog.korn123.easydiary.R
import me.blog.korn123.easydiary.extensions.*
import me.blog.korn123.easydiary.helper.*
import me.blog.korn123.easydiary.models.ActionLog
import me.blog.korn123.easydiary.services.BaseNotificationService
import me.blog.korn123.easydiary.services.NotificationService
import org.apache.commons.io.FilenameUtils
import java.io.File


open class BaseDevActivity : EasyDiaryActivity() {

    /***************************************************************************************************
     *   global properties
     *
     ***************************************************************************************************/


    /***************************************************************************************************
     *   override functions
     *
     ***************************************************************************************************/
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dev)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            title = "Easy-Diary Dev Mode"
            setDisplayHomeAsUpEnabled(true)
        }

        updateActionLog()

        nextAlarm.setOnClickListener {
            val nextAlarm = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val triggerTimeMillis = (getSystemService(Context.ALARM_SERVICE) as AlarmManager).nextAlarmClock?.triggerTime ?: 0
                when (triggerTimeMillis > 0) {
                    true -> DateUtils.getFullPatternDateWithTime(triggerTimeMillis)
                    false -> "Alarm info is not exist."
                }
            } else {
                Settings.System.getString(contentResolver, Settings.System.NEXT_ALARM_FORMATTED)
            }

            toast(nextAlarm, Toast.LENGTH_LONG)
        }

        notification1.setOnClickListener {
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
                notify(NOTIFICATION_ID_DEV, createNotification(NotificationInfo(R.drawable.ic_diary_writing, true)))
            }
        }
        notification2.setOnClickListener {
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
                notify(NOTIFICATION_ID_DEV, createNotification(NotificationInfo(R.drawable.ic_diary_backup_local, useActionButton = true, useCustomContentView = true)))
            }
        }

        clearLog.setOnClickListener {
            EasyDiaryDbHelper.deleteActionLogAll()
            updateActionLog()
        }

        clearUnusedPhoto.setOnClickListener {
            val localPhotoBaseNames = arrayListOf<String>()
            val unUsedPhotos = arrayListOf<String>()
            File(EasyDiaryUtils.getApplicationDataDirectory(this) + DIARY_PHOTO_DIRECTORY).listFiles().map {
                localPhotoBaseNames.add(it.name)
            }

            EasyDiaryDbHelper.selectPhotoUriAll().map { photoUriDto ->
                if (!localPhotoBaseNames.contains(FilenameUtils.getBaseName(photoUriDto.getFilePath()))) {
                    unUsedPhotos.add(FilenameUtils.getBaseName(photoUriDto.getFilePath()))
                }
            }
            showAlertDialog(unUsedPhotos.size.toString(), null, true)
        }

        locationManager.setOnClickListener {
            fun setLocationInfo() {
                getLastKnownLocation()?.let {
                    var info = "Longitude: ${it.longitude}\nLatitude: ${it.latitude}\n"
                    getFromLocation(it.latitude, it.longitude, 1)?.let { address ->
                        if (address.isNotEmpty()) {
                            info += fullAddress(address[0])
                        }
                    }
                    locationManagerInfo.text = info
                }
            }
            when (hasGPSPermissions()) {
                true -> setLocationInfo()
                false -> {
                    acquireGPSPermissions() {
                        setLocationInfo()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        pauseLock()

        when (requestCode) {
            REQUEST_CODE_ACTION_LOCATION_SOURCE_SETTINGS -> {
                makeSnackBar(if (isLocationEnabled()) "GPS provider setting is activated." else "The request operation did not complete normally.")
            }
        }
    }


    /***************************************************************************************************
     *   etc functions
     *
     ***************************************************************************************************/
    private fun updateActionLog() {
        val actionLogs: List<ActionLog> = EasyDiaryDbHelper.readActionLogAll()
        val sb = StringBuilder()
        actionLogs.map {
            sb.append("${it.className}-${it.signature}-${it.key}: ${it.value}\n")
        }
        actionLog.text = sb.toString()
    }

    @SuppressLint("NewApi")
    private fun createNotification(notificationInfo: NotificationInfo): Notification {
        if (isOreoPlus()) {
            // Create the NotificationChannel
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("${NOTIFICATION_CHANNEL_ID}_dev", "${NOTIFICATION_CHANNEL_NAME}_dev", importance)
            channel.description = NOTIFICATION_CHANNEL_DESCRIPTION

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Notification Title"
        val text = "알림에 대한 본문 내용이 들어가는 영역입니다. 기본 템플릿을 확장형 알림을 구현할 수 있습니다."
        val notificationBuilder = NotificationCompat.Builder(applicationContext, "${NOTIFICATION_CHANNEL_ID}_dev")
        notificationBuilder
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_easydiary)
                .setLargeIcon(BitmapFactory.decodeResource(resources, notificationInfo.largeIconResourceId))
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(
                        PendingIntent.getActivity(this, 0, Intent(this, DiaryMainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }, PendingIntent.FLAG_UPDATE_CURRENT)
                )


        if (notificationInfo.useCustomContentView) {
            notificationBuilder
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(RemoteViews(applicationContext.packageName, R.layout.layout_notification))
        } else {
            notificationBuilder
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text).setSummaryText(title))
        }

        if (notificationInfo.useActionButton) {
            notificationBuilder.addAction(
                    R.drawable.ic_easydiary,
                    getString(R.string.dismiss),
                    PendingIntent.getService(this, 0, Intent(this, NotificationService::class.java).apply {
                        action = BaseNotificationService.ACTION_DISMISS_DEV
                    }, 0)
            )
        }

        return notificationBuilder.build()
    }

    companion object {
        const val NOTIFICATION_ID_DEV = 9999
    }
}


/***************************************************************************************************
 *   classes
 *
 ***************************************************************************************************/
data class NotificationInfo(var largeIconResourceId: Int, var useActionButton: Boolean = false, var useCustomContentView: Boolean = false)


/***************************************************************************************************
 *   extensions
 *
 ***************************************************************************************************/
fun fun1(param1: String, block: (responseData: String) -> String): String {
    println(param1)
    return block("")
}

fun test1() {
    val result = fun1("banana") { responseData ->
        "data: $responseData"
    }
    println(result)
}





