package com.hassan.dev.vipcaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {

            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (number != null) {
                    val ringtoneUri = getCustomRingtone(context, number)

                    if (ringtoneUri != null) {
                        playTone(context, ringtoneUri)
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                stopTone()
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                stopTone()
            }
        }
    }

    private fun getCustomRingtone(context: Context, phone: String): Uri? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phone)
        )

        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.CUSTOM_RINGTONE),
            null,
            null,
            null
        )?.use {
            if (it.moveToFirst()) {
                val tone = it.getString(0)
                if (!tone.isNullOrEmpty()) {
                    return Uri.parse(tone)
                }
            }
        }

        return null
    }

    private fun playTone(context: Context, uri: Uri) {
        stopTone()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }
    }

    private fun stopTone() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}