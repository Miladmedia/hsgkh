package ir.heyat.khalilshahr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class FeedWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when(val result = FeedRepository(applicationContext).fetch()) {
        is FeedRepository.Fetch.Failure -> Result.retry()
        is FeedRepository.Fetch.Success -> { if (!result.isBaseline) notifyNew(result.newIds.size); Result.success() }
    }
    private fun notifyNew(count: Int) {
        if (count == 0) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("feed_updates", "خبرهای تازه", NotificationManager.IMPORTANCE_DEFAULT))
        val n = NotificationCompat.Builder(applicationContext,"feed_updates").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("خبر تازه از کانال منبع").setContentText("$count پست تازه دریافت شد").setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(6102,n) }
    }
}
