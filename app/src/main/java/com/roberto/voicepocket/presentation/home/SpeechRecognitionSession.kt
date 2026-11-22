package com.roberto.voicepocket.presentation.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

internal sealed interface VoiceTarget {
    data object NewIdea : VoiceTarget
    data class Edit(val ideaId: Long) : VoiceTarget
}

internal enum class VoicePhase(val message: String) {
    Permission("Esperando permiso del micrófono…"),
    Starting("Preparando micrófono..."),
    Listening("Escuchando..."),
    Processing("Procesando..."),
    Error("")
}

internal data class VoiceState(
    val target: VoiceTarget,
    val phase: VoicePhase,
    val message: String = phase.message,
    val partial: String = ""
) {
    val busy: Boolean get() = phase != VoicePhase.Error
}

// All calls are made on the main thread, including Android's recognition callbacks.
internal class SpeechRecognitionSession(
    private val createRecognizer: () -> VoiceRecognizer?,
    private val onResult: (VoiceTarget, String) -> Unit
) {
    var state by mutableStateOf<VoiceState?>(null)
        private set

    private var active: VoiceRecognizer? = null
    private var permissionInFlight = false

    fun request(target: VoiceTarget, hasPermission: Boolean, launchPermission: () -> Unit) {
        if (permissionInFlight || state?.busy == true) return
        if (hasPermission) {
            start(target)
        } else {
            permissionInFlight = true
            state = VoiceState(target, VoicePhase.Permission)
            launchPermission()
        }
    }

    fun permissionResult(granted: Boolean, canStart: (VoiceTarget) -> Boolean) {
        if (!permissionInFlight) return
        permissionInFlight = false
        val pending = state?.takeIf { it.phase == VoicePhase.Permission } ?: return
        state = null
        if (!canStart(pending.target)) return
        if (granted) start(pending.target)
        else fail(pending.target, "Se necesita permiso para usar el micrófono")
    }

    fun stop(target: VoiceTarget) {
        val current = state ?: return
        val recognizer = active ?: return
        if (current.target != target || current.phase != VoicePhase.Listening) return
        state = current.copy(phase = VoicePhase.Processing, message = VoicePhase.Processing.message)
        try {
            recognizer.stop()
        } catch (_: RuntimeException) {
            if (active === recognizer) {
                cancel()
                fail(target, "No fue posible detener el reconocimiento")
            }
        }
    }

    fun cancel() {
        val recognizer = active
        active = null
        state = null
        recognizer?.close()
    }

    private fun start(target: VoiceTarget) {
        state = VoiceState(target, VoicePhase.Starting)
        try {
            val recognizer = createRecognizer()
            if (recognizer == null) {
                fail(target, "El reconocimiento de voz no está disponible")
                return
            }
            active = recognizer
            recognizer.start(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (active === recognizer && state?.phase == VoicePhase.Starting) {
                        state = VoiceState(target, VoicePhase.Listening)
                    }
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onEndOfSpeech() {
                    if (active === recognizer) {
                        state = state?.copy(
                            phase = VoicePhase.Processing,
                            message = VoicePhase.Processing.message
                        )
                    }
                }

                override fun onError(error: Int) {
                    if (active !== recognizer) return
                    cancel()
                    fail(target, speechErrorMessage(error))
                }

                override fun onResults(results: Bundle?) {
                    if (active !== recognizer) return
                    val text = results.firstTranscription()
                    cancel()
                    if (text.isBlank()) fail(target, "No se reconoció ninguna frase")
                    else onResult(target, text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (active !== recognizer) return
                    val text = partialResults.firstTranscription()
                    if (text.isNotBlank()) state = state?.copy(partial = text)
                }
            })
        } catch (_: RuntimeException) {
            cancel()
            fail(target, "No fue posible iniciar el reconocimiento")
        }
    }

    private fun fail(target: VoiceTarget, message: String) {
        state = VoiceState(target, VoicePhase.Error, message)
    }
}

internal interface VoiceRecognizer {
    fun start(listener: RecognitionListener)
    fun stop()
    fun close()
}

internal fun androidVoiceRecognizer(context: Context): VoiceRecognizer? {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) return null
    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    return object : VoiceRecognizer {
        override fun start(listener: RecognitionListener) {
            recognizer.setRecognitionListener(listener)
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            })
        }

        override fun stop() = recognizer.stopListening()
        override fun close() = recognizer.destroy()
    }
}

private fun Bundle?.firstTranscription(): String =
    this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()

private fun speechErrorMessage(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_AUDIO -> "Ocurrió un problema con el micrófono"
    SpeechRecognizer.ERROR_CLIENT -> "No fue posible iniciar el reconocimiento"
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "No existe permiso para usar el micrófono"
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Revisa tu conexión e inténtalo nuevamente"
    SpeechRecognizer.ERROR_NO_MATCH -> "No entendí la frase. Inténtalo otra vez"
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor está ocupado. Inténtalo nuevamente"
    SpeechRecognizer.ERROR_SERVER -> "El servicio de voz no está disponible"
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No escuché ninguna voz"
    else -> "No fue posible reconocer la voz"
}
