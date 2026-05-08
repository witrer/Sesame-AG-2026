package fansirsqi.xposed.sesame.hook

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.TimeUtil

/**
 * 调度器 - 基于 AlarmManager 实现
 *
 * 优势：
 * 1. 精确唤醒 - 使用 setExactAndAllowWhileIdle 确保在 Doze 模式下也能准时执行。
 * 2. 系统级调度 - 可靠性高，由 Android 系统保证。
 * 3. 功耗优化 - 任务执行完后设备可以立刻返回休眠状态。
 *
 * 替换了原有的 CoroutineScheduler，以解决 Doze 模式下的执行延迟问题。
 */
class CoroutineScheduler(private val context: Context) {

    companion object {
        private const val TAG = "AlarmScheduler" // 更名为 AlarmScheduler 以反映实现
        private const val MAIN_TASK_REQUEST_CODE = 12345
        private const val PRE_WAKEUP_REQUEST_CODE = 12346
        private const val WAKEUP_REQUEST_CODE_OFFSET = 20000
        private const val MAX_WAKEUP_ALARMS = 30 // 假设最多有30个唤醒闹钟
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun getPendingIntent(requestCode: Int, intent: Intent, flags: Int): PendingIntent? {
        // 由于 minSdk >= 24 (Android N)，我们总是在 API 23 (M) 或更高版本上运行。
        // FLAG_IMMUTABLE 是在 API 23 中添加的，并且在应用目标为 API 31 (S) 或更高版本时是必需的。
        // 因此，在这里始终添加此标志是安全且正确的。
        val finalFlags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, finalFlags)
    }

    /**
     * 检查应用是否具有调度精确闹钟的权限。
     * 从 Android 12 (S) 开始需要此权限。
     * @return 如果可以调度精确闹钟，则返回 true；否则返回 false。
     */
    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            // 在 Android 12 以下版本，此权限不是必需的。
            true
        }
    }

    /**
     * 调度精确时间执行
     */
    fun scheduleExactExecution(delayMillis: Long, exactTimeMillis: Long) {
        if (!canScheduleExactAlarms()) {
            Log.error(TAG, "❌ 无法调度精确执行任务：缺少 SCHEDULE_EXACT_ALARM 权限。请在系统设置中为应用开启“闹钟和提醒”权限。")
            return
        }
        // 计划在任务执行前1分钟触发预唤醒
        val preWakeupTime = exactTimeMillis - 60 * 1000
        if (preWakeupTime <= System.currentTimeMillis()) {
            Log.record(TAG, "预唤醒时间已过，立即执行")
            // 如果预唤醒时间已过，直接发送广播立即执行
            val intent = Intent("com.eg.android.AlipayGphone.sesame.execute").apply {
                putExtra("alarm_triggered", true)
                setPackage(General.PACKAGE_NAME)
            }
            context.sendBroadcast(intent)
            return
        }

        val intent = Intent("com.eg.android.AlipayGphone.sesame.prewakeup").apply {
            putExtra("execution_time", exactTimeMillis)
            setPackage(General.PACKAGE_NAME)
        }
        val pendingIntent = getPendingIntent(PRE_WAKEUP_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (pendingIntent == null) {
            Log.error(TAG, "❌ 无法创建用于调度任务的 PendingIntent")
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preWakeupTime, pendingIntent)
        Log.record(
            TAG,
            "⏰ 已调度预唤醒任务 (主任务) | 预定: ${TimeUtil.getCommonDate(preWakeupTime)}"
        )
    }

    /**
     * 调度延迟执行
     */
    fun scheduleDelayedExecution(delayMillis: Long) {
        val targetTime = System.currentTimeMillis() + delayMillis
        scheduleExactExecution(delayMillis, targetTime)
    }

    /**
     * 调度预唤醒任务，在主任务执行前1分钟触发
     */
    private fun schedulePreWakeup(exactTimeMillis: Long) {
        // 在主任务前1分钟唤醒
        val preWakeupTime = exactTimeMillis - 60 * 1000
        if (preWakeupTime > System.currentTimeMillis()) {
            val intent = Intent("com.eg.android.AlipayGphone.sesame.prewakeup").apply {
                setPackage(General.PACKAGE_NAME)
            }
            val pendingIntent = getPendingIntent(
                PRE_WAKEUP_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            if (pendingIntent != null) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    preWakeupTime,
                    pendingIntent
                )
                Log.record(TAG, "⏰ 已调度预唤醒任务 | 预定: ${TimeUtil.getCommonDate(preWakeupTime)}")
            }
        }
    }

    /**
     * 调度唤醒任务（例如0点定时）
     */
    fun scheduleWakeupAlarm(
        triggerAtMillis: Long,
        requestCode: Int,
        isMainAlarm: Boolean
    ): Boolean {
        if (!canScheduleExactAlarms()) {
            Log.error(TAG, "❌ 无法调度唤醒任务：缺少 SCHEDULE_EXACT_ALARM 权限。请在系统设置中为应用开启“闹钟和提醒”权限。")
            return false
        }
        return try {
            val intent = Intent("com.eg.android.AlipayGphone.sesame.execute").apply {
                putExtra("alarm_triggered", true)
                putExtra("waken_at_time", true)
                if (!isMainAlarm) {
                    putExtra("waken_time", TimeUtil.getTimeStr(triggerAtMillis))
                }
                setPackage(General.PACKAGE_NAME)
            }
            val pendingIntent = getPendingIntent(
                WAKEUP_REQUEST_CODE_OFFSET + requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            if (pendingIntent == null) {
                Log.error(TAG, "❌ 无法创建用于调度唤醒任务的 PendingIntent")
                return false
            }

            // 由于 minSdk >= 24，因此可以直接使用 setExactAndAllowWhileIdle。
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

            val taskType = if (isMainAlarm) "主定时" else "自定义定时"
            Log.record(
                TAG,
                "⏰ ${taskType}任务调度成功 (闹钟调度器) | ID=$requestCode | 触发: ${TimeUtil.getCommonDate(triggerAtMillis)}"
            )
            true
        } catch (e: Exception) {
            Log.error(TAG, "调度唤醒任务失败: ${e.message}")
            Log.printStackTrace(TAG, e)
            false
        }
    }

    /**
     * 取消所有唤醒任务
     */
    fun cancelAllWakeupAlarms() {
        Log.record(TAG, "正在取消所有唤醒任务...")
        for (i in 0..MAX_WAKEUP_ALARMS) {
            val intent = Intent("com.eg.android.AlipayGphone.sesame.execute")
            intent.setPackage(General.PACKAGE_NAME)
            val pendingIntent = getPendingIntent(
                WAKEUP_REQUEST_CODE_OFFSET + i,
                intent,
                PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
        Log.record(TAG, "已尝试取消所有唤醒任务")
    }

    /**
     * 取消所有任务
     */
    fun cancelAll() {
        val intent = Intent("com.eg.android.AlipayGphone.sesame.execute")
        intent.setPackage(General.PACKAGE_NAME)
        val pendingIntent = getPendingIntent(MAIN_TASK_REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
        val preWakeupIntent = Intent("com.eg.android.AlipayGphone.sesame.prewakeup")
        preWakeupIntent.setPackage(General.PACKAGE_NAME)
        val preWakeupPendingIntent =
            getPendingIntent(PRE_WAKEUP_REQUEST_CODE, preWakeupIntent, PendingIntent.FLAG_NO_CREATE)
        if (preWakeupPendingIntent != null) {
            alarmManager.cancel(preWakeupPendingIntent)
            Log.record(TAG, "已取消预唤醒任务")
        }
        cancelAllWakeupAlarms()
        Log.record(TAG, "已取消所有调度任务")
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        Log.record(TAG, "🧹 开始清理调度器资源")
        cancelAll()
        Log.record(TAG, "✅ 调度器资源清理完成")
    }
}

