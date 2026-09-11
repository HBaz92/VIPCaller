package com.hassan.dev.vipcaller.core

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * تشغيل نغمة الـ VIP أثناء الرنين وإيقافها فور الرد أو الإنهاء.
 * يُستخدم من [com.hassan.dev.vipcaller.CallReceiver] ومن الخدمة الأمامية معًا،
 * لذلك كل الحالة هنا ثابتة ومحمية بقفل واحد.
 */
object RingtonePlayer {

    private const val TAG = "VipRingtonePlayer"

    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var focusRequest: Any? = null
    private var stopRunnable: Runnable? = null

    /** مستوى رنين النظام قبل الكتم، لإعادته كما كان بعد انتهاء المكالمة. */
    private var savedRingVolume: Int? = null
    private var savedRingerMode: Int? = null

    val isPlaying: Boolean
        get() = synchronized(lock) { player != null }

    fun play(
        context: Context,
        uri: Uri?,
        vibrate: Boolean,
        ignoreSilent: Boolean,
        maxSeconds: Int,
        silenceSystemRinger: Boolean
    ) {
        val app = context.applicationContext
        val tone = uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) ?: return

        synchronized(lock) {
            stopInternal(app)

            val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            // في الوضع الصامت نستخدم مسار المنبّه لأنه يتجاوز كتم الرنين على معظم الأجهزة.
            val usage =
                if (ignoreSilent) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_RINGTONE

            val attributes = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // أندرويد يشغّل نغمة جهة الاتصال بنفسه، فبدون الكتم تُسمع نغمتان متداخلتان.
            if (silenceSystemRinger) silenceRinger(app, audioManager)

            requestFocus(audioManager, attributes)

            val created = runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(attributes)
                    setDataSource(app, tone)
                    isLooping = true
                    prepare()
                    start()
                }
            }.getOrElse { error ->
                Log.w(TAG, "تعذّر تشغيل النغمة $tone", error)
                abandonFocus(audioManager)
                null
            }

            player = created

            if (created != null) {
                if (vibrate) startVibration(app)
                scheduleAutoStop(app, maxSeconds)
            }
        }
    }

    fun stop(context: Context) {
        synchronized(lock) { stopInternal(context.applicationContext) }
    }

    private fun stopInternal(app: Context) {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null

        player?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.release() }
        }
        player = null

        vibrator?.let { runCatching { it.cancel() } }
        vibrator = null

        val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        restoreRinger(audioManager)
        abandonFocus(audioManager)
    }

    // --------------------------------------------------------- system ringer

    private fun silenceRinger(app: Context, audioManager: AudioManager?) {
        audioManager ?: return

        // كتم مسار الرنين يغيّر وضع الجهاز، وهذا يتطلب إذن الوصول لوضع عدم الإزعاج.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = app.getSystemService(NotificationManager::class.java)
            if (manager?.isNotificationPolicyAccessGranted != true) {
                Log.w(TAG, "إذن عدم الإزعاج غير ممنوح — سيبقى رنين النظام مسموعًا")
                return
            }
        }

        runCatching {
            savedRingerMode = audioManager.ringerMode
            savedRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        }.onFailure { error ->
            Log.w(TAG, "تعذّر كتم رنين النظام", error)
            // لا نترك قيمًا محفوظة نصف مكتملة تُعيد ضبط الجهاز على وضع خاطئ.
            savedRingerMode = null
            savedRingVolume = null
        }
    }

    private fun restoreRinger(audioManager: AudioManager?) {
        audioManager ?: return

        // الترتيب مهم: إعادة المستوى أولًا لأنها قد تغيّر الوضع، ثم تثبيت الوضع الأصلي.
        savedRingVolume?.let { volume ->
            runCatching { audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0) }
                .onFailure { Log.w(TAG, "تعذّرت إعادة مستوى الرنين", it) }
        }
        savedRingerMode?.let { mode ->
            runCatching { audioManager.ringerMode = mode }
                .onFailure { Log.w(TAG, "تعذّرت إعادة وضع الرنين", it) }
        }

        savedRingVolume = null
        savedRingerMode = null
    }

    /** شبكة أمان: تُعيد رنين النظام حتى لو لم تكن هناك نغمة تعمل. */
    fun restoreSystemRinger(context: Context) {
        synchronized(lock) {
            val app = context.applicationContext
            restoreRinger(app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
        }
    }

    private fun scheduleAutoStop(app: Context, maxSeconds: Int) {
        if (maxSeconds <= 0) return
        val runnable = Runnable { stop(app) }
        stopRunnable = runnable
        handler.postDelayed(runnable, maxSeconds * 1000L)
    }

    // ------------------------------------------------------------------ audio

    @Suppress("DEPRECATION")
    private fun requestFocus(audioManager: AudioManager?, attributes: AudioAttributes) {
        audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = android.media.AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonFocus(audioManager: AudioManager?) {
        audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (focusRequest as? android.media.AudioFocusRequest)?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(null)
        }
        focusRequest = null
    }

    // -------------------------------------------------------------- vibration

    @Suppress("DEPRECATION")
    private fun startVibration(app: Context) {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        val pattern = longArrayOf(0, 500, 700)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                vib.vibrate(pattern, 0)
            }
        }
        vibrator = vib
    }
}
