package com.roberto.voicepocket

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roberto.voicepocket.presentation.home.SpeechRecognitionSession
import com.roberto.voicepocket.presentation.home.VoicePhase
import com.roberto.voicepocket.presentation.home.VoiceRecognizer
import com.roberto.voicepocket.presentation.home.VoiceTarget
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeechRecognitionSessionTest {
    @Test
    fun sharesOneSessionAndRoutesOnlyOneFinalResult() = onMain {
        val recognizers = mutableListOf<FakeRecognizer>()
        val results = mutableListOf<Pair<VoiceTarget, String>>()
        val session = SpeechRecognitionSession(
            { FakeRecognizer().also { recognizers.add(it) } },
            { target, text -> results.add(target to text) }
        )
        val editor = VoiceTarget.Edit(42)
        session.request(editor, true) { error("Unexpected permission request") }
        session.request(VoiceTarget.NewIdea, true) {}
        val first = recognizers.single()
        first.listener.onReadyForSpeech(null)
        first.listener.onPartialResults(transcription("Parcial"))
        assertEquals("Parcial", session.state?.partial)
        assertEquals(0, results.size)
        session.stop(VoiceTarget.NewIdea)
        session.stop(editor)
        session.stop(editor)
        assertEquals(1, first.stops)
        first.listener.onResults(transcription("Final"))
        first.listener.onResults(transcription("Duplicado"))
        assertEquals(listOf(editor to "Final"), results)
        assertEquals(1, first.closes)
    }

    @Test
    fun ignoresPermissionAfterCancellationAndHandlesDenial() = onMain {
        var launches = 0
        var starts = 0
        val session = SpeechRecognitionSession(
            { starts++; FakeRecognizer() },
            { _, _ -> error("Unexpected result") }
        )
        val editor = VoiceTarget.Edit(42)
        session.request(editor, false) { launches++ }
        session.cancel()
        session.request(VoiceTarget.NewIdea, false) { launches++ }
        session.permissionResult(true) { true }
        assertEquals(1, launches)
        assertEquals(0, starts)
        assertEquals(null, session.state)

        session.request(editor, false) { launches++ }
        session.permissionResult(false) { it == editor }
        assertEquals(2, launches)
        assertEquals(0, starts)
        assertEquals(VoicePhase.Error, session.state?.phase)
        assertEquals(editor, session.state?.target)
    }

    @Test
    fun ignoresCallbacksAfterCancellationAndClearsPartialOnError() = onMain {
        val recognizers = mutableListOf<FakeRecognizer>()
        val results = mutableListOf<String>()
        val session = SpeechRecognitionSession(
            { FakeRecognizer().also { recognizers.add(it) } },
            { _, text -> results.add(text) }
        )
        val editor = VoiceTarget.Edit(42)
        session.request(editor, true) {}
        session.cancel()
        session.request(VoiceTarget.NewIdea, true) {}
        recognizers.first().listener.onResults(transcription("Tardío"))
        val current = recognizers.last()
        current.listener.onPartialResults(transcription("Parcial"))
        current.listener.onError(SpeechRecognizer.ERROR_NETWORK)
        current.listener.onResults(transcription("Después del error"))
        assertEquals(VoiceTarget.NewIdea, session.state?.target)
        assertEquals(VoicePhase.Error, session.state?.phase)
        assertEquals("", session.state?.partial)
        assertEquals(emptyList<String>(), results)
    }

    private fun onMain(test: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync { test() }

    private fun transcription(text: String) = Bundle().apply {
        putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text))
    }

    private class FakeRecognizer : VoiceRecognizer {
        lateinit var listener: RecognitionListener
        var stops = 0
        var closes = 0

        override fun start(listener: RecognitionListener) {
            this.listener = listener
        }

        override fun stop() { stops++ }
        override fun close() { closes++ }
    }
}
