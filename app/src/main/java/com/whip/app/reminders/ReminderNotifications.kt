package com.whip.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.domain.WhipTask
import com.whip.app.core.WhipLaunchActions

object ReminderNotifications {
    const val CHANNEL_ID = "task_reminders"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Task reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminders for scheduled Whip tasks"
            },
        )
    }

    fun show(
        context: Context,
        task: WhipTask,
        originalEpochDay: Long?,
        allowDirectCompletion: Boolean = true,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
            .setAction(WhipLaunchActions.ACTION_OPEN_TASK)
            .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, task.id)
            .putExtra(
                WhipLaunchActions.EXTRA_OCCURRENCE_EPOCH_DAY,
                originalEpochDay ?: Long.MIN_VALUE,
            )
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            (task.id xor (originalEpochDay ?: 0L)).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText("Scheduled task is due")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .apply {
                val actionToken = System.currentTimeMillis()
                if (allowDirectCompletion) {
                    addAction(
                        R.drawable.ic_notification,
                        "Complete",
                        PendingIntent.getBroadcast(
                            context,
                            (task.id * 31L + (originalEpochDay ?: 0L)).hashCode(),
                            Intent(context, ReminderActionReceiver::class.java)
                                .setAction(ReminderActionReceiver.ACTION_COMPLETE)
                                .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, task.id)
                                .putExtra(
                                    ReminderActionReceiver.EXTRA_ORIGINAL_EPOCH_DAY,
                                    originalEpochDay ?: Long.MIN_VALUE,
                                )
                                .putExtra(ReminderActionReceiver.EXTRA_ACTION_TOKEN, actionToken),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                } else {
                    addAction(R.drawable.ic_notification, "Review subtasks", pendingIntent)
                }
                addAction(
                    R.drawable.ic_notification,
                    "Snooze 10 min",
                    PendingIntent.getBroadcast(
                        context,
                        (task.id * 47L + (originalEpochDay ?: 0L)).hashCode(),
                        Intent(context, ReminderActionReceiver::class.java)
                            .setAction(ReminderActionReceiver.ACTION_SNOOZE)
                            .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, task.id)
                            .putExtra(ReminderActionReceiver.EXTRA_ORIGINAL_EPOCH_DAY, originalEpochDay ?: Long.MIN_VALUE)
                            .putExtra(ReminderActionReceiver.EXTRA_ACTION_TOKEN, actionToken),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
            .build()

        NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
    }

    fun showCompletionUndo(context: Context, task: WhipTask, originalEpochDay: Long?) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val undo = PendingIntent.getBroadcast(
            context,
            (task.id * 53L + (originalEpochDay ?: 0L)).hashCode(),
            Intent(context, ReminderActionReceiver::class.java)
                .setAction(ReminderActionReceiver.ACTION_UNDO)
                .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, task.id)
                .putExtra(ReminderActionReceiver.EXTRA_ORIGINAL_EPOCH_DAY, originalEpochDay ?: Long.MIN_VALUE)
                .putExtra(ReminderActionReceiver.EXTRA_ACTION_TOKEN, System.currentTimeMillis()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${task.title} completed")
            .setContentText("Completion recorded")
            .setAutoCancel(true)
            .addAction(R.drawable.ic_notification, "Undo", undo)
            .build()
        NotificationManagerCompat.from(context).notify(completionUndoNotificationId(task.id), notification)
    }

    fun completionUndoNotificationId(taskId: Long): Int = (taskId * 59L + 11L).hashCode()

    fun showTest(context: Context): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        createChannel(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0x57484950,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Whip notifications are working")
            .setContentText("This is a test notification from Settings.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
        return true
    }

    fun showLocation(context: Context, task: WhipTask) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val location = task.locationReminder ?: return
        val pendingIntent = PendingIntent.getActivity(
            context,
            (task.id * 17L).hashCode(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val verb = if (location.trigger == com.whip.app.domain.LocationTrigger.Arrive) "arrived at" else "left"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText("You $verb ${location.name}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify((task.id * 37L + 7L).hashCode(), notification)
    }

    private const val TEST_NOTIFICATION_ID = 0x57484950
}
