package com.example.testservicecomponentandroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FocusService : Service() {

    enum class TimerState {
        STOPPED, RUNNING, PAUSED
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): FocusService = this@FocusService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private val _secondsRemaining = MutableStateFlow(INITIAL_TIME)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.STOPPED)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "focus_timer_channel"
        const val INITIAL_TIME = 1500
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> stopTimer()
        }
        return START_STICKY
    }

    private fun startTimer() {
        if (_timerState.value == TimerState.RUNNING) return

        _timerState.value = TimerState.RUNNING
        startForeground(NOTIFICATION_ID, buildNotification())

        timerJob = serviceScope.launch {
            while (_secondsRemaining.value > 0 && _timerState.value == TimerState.RUNNING) {
                delay(1000)
                _secondsRemaining.value -= 1
                updateNotification()
            }
            if (_secondsRemaining.value == 0) {
                _timerState.value = TimerState.STOPPED
                _secondsRemaining.value = INITIAL_TIME
                updateNotification(finished = true)
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }
    }

    private fun pauseTimer() {
        if (_timerState.value != TimerState.RUNNING) return

        _timerState.value = TimerState.PAUSED
        timerJob?.cancel()
        updateNotification()
    }

    private fun stopTimer() {
        _timerState.value = TimerState.STOPPED
        _secondsRemaining.value = INITIAL_TIME
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temporizador de Foco",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações do aplicativo de foco Pomodoro"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(finished: Boolean = false): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, FocusService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startIntent = Intent(this, FocusService::class.java).apply { action = ACTION_START }
        val startPendingIntent = PendingIntent.getService(
            this, 2, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTime = formatTime(_secondsRemaining.value)
        val contentText = when {
            finished -> "Sessão concluída! Excelente trabalho."
            _timerState.value == TimerState.RUNNING -> "Foco ativo: $formattedTime"
            else -> "Temporizador pausado"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Temporizador Pomodoro")
            .setContentText(contentText)
            .setOngoing(_timerState.value == TimerState.RUNNING)
            .setContentIntent(openAppPendingIntent)
            .setOnlyAlertOnce(true)

        if (!finished) {
            if (_timerState.value == TimerState.RUNNING) {
                builder.addAction(android.R.drawable.ic_media_pause, "Pausar", pausePendingIntent)
            } else if (_timerState.value == TimerState.PAUSED) {
                builder.addAction(android.R.drawable.ic_media_play, "Retomar", startPendingIntent)
            }
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Parar", stopPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification(finished: Boolean = false) {
        if (_timerState.value != TimerState.STOPPED || finished) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(finished))
        }
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }
}