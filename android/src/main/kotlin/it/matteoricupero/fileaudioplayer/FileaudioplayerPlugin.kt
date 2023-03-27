package it.matteoricupero.fileaudioplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Build.VERSION
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.IOException

public class FileaudioplayerPlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel

    private var players: HashMap<String, Player> = HashMap();
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "fileaudioplayer")

        audioManager = flutterPluginBinding.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        channel.setMethodCallHandler(this)
    }

    companion object {

        @JvmStatic
        var audioManager: AudioManager? = null

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "fileaudioplayer")

            audioManager = registrar.activeContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

            channel.setMethodCallHandler(FileaudioplayerPlugin())
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        var channel: String? = call.argument("channel")
        if(channel == null) {
            channel = "default";
        }
        if(!players.containsKey(channel)) {
          players.put(channel, Player())
        }
        var player : Player? = players.get(channel);
        if(player == null) { // shouldn't be possible
            result.error("player is null", null, null)
            return
        }

        val action = call.method
        when (action) {
            "start" -> {
                val url : String? = call.argument("url")
                if (url != null) {
                    player.initializePlayer(url, result)
                    player.startPlayer()
                } else {
                    result.error("no file", null, null)
                }
            }
            "stop" -> player.stopPlayer(result)
            "pause" -> player.pausePlayer(result)
            else -> player.resumePlayer(result)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    inner class Player {
        private var player: MediaPlayer? = null
        private var result: Result? = null

        public fun initializePlayer(url: String, result: Result) {
            this.result = result
            try {
                if (player != null) {
                    player?.stop()
                    player?.reset()
                    player?.release()
                    player = null
                }

                player = MediaPlayer()

                if (VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    player?.setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                } else {
                    player?.setAudioStreamType(AudioManager.STREAM_SYSTEM)
                }

                player?.setDataSource(url)

            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        public fun startPlayer() {
            try {
                if (player != null) {
                    requestFocus()
                    player?.prepareAsync()
                    player?.setOnPreparedListener {
                        try {
                            player?.start()
                        } catch (e: IllegalStateException) {
                            afterException(e, result)
                        }
                    }
                    player?.setOnCompletionListener {
                        try {
                            abandonFocus()
                            result?.success(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    result?.error("player null", null, null)
                }
            } catch (e: Exception) {
                afterException(e, result)
            }
        }

        private fun requestFocus() {
            if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val mPlaybackAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                            .setAudioAttributes(mPlaybackAttributes)
                            .setOnAudioFocusChangeListener { }
                            .build()
                    audioManager?.requestAudioFocus(audioFocusRequest)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun abandonFocus() {
            try {
                if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioManager?.abandonAudioFocusRequest(audioFocusRequest)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        public fun stopPlayer(r: Result) {
            try {
                if (player != null) {
                    abandonFocus()
                    player?.stop()
                    player?.reset()
                    player?.release()
                    player = null
                }
                result?.success(true)
                r?.success(true)
            } catch (e: Exception) {
                afterException(e, r)
            }
        }

        public fun pausePlayer(result: Result) {
            try {
                if (player!!.isPlaying) {
                    abandonFocus()
                    player?.pause()
                }
                result?.success(true)
            } catch (e: Exception) {
                afterException(e, result)
            }
        }

        public fun resumePlayer(result: Result) {
            try {
                if (player != null && !player!!.isPlaying) {
                    abandonFocus()
                    player?.start()
                }
                result?.success(true)
            } catch (e: Exception) {
                afterException(e, result)
            }
        }

        private fun afterException(e: Exception, result: Result?) {
            e.printStackTrace()
            abandonFocus()
            result?.error(e.message ?: "Error", e.message ?: "Error", e.cause ?: "")
        }
    }
}
