package com.yanchyshyn.smartmethodstest.activity

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.github.kittinunf.result.Result
import com.yanchyshyn.smartmethods.SmartMethod
import com.yanchyshyn.smartmethodstest.R
import com.yanchyshyn.smartmethodstest.asynctask.GetTimeAsyncTask
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

	// region Smart Methods
	private val speakTimeSmartMethod = MainActivity_SpeakTime(this::speakTime)
	// endregion

	// this field gets non null value only for period of service connection
	private var tts: TextToSpeech? = null
	private var getTimeAsyncTask: GetTimeAsyncTask? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		btnGetTime.setOnClickListener {
			initTts()
			getTime()
		}
	}

	override fun onDestroy() {
		tts?.shutdown()

		speakTimeSmartMethod.isEnabled = false
		speakTimeSmartMethod.tts?.shutdown()
		speakTimeSmartMethod.clear()

		getTimeAsyncTask?.cancel(true)

		super.onDestroy()
	}

	@SmartMethod
	private fun speakTime(tts: TextToSpeech, date: Date) {
		tts.speak(date.toString(), TextToSpeech.QUEUE_FLUSH, null, "")
	}

	private fun initTts() {
		if ((tts != null) || speakTimeSmartMethod.isTtsSet) return

		tts = TextToSpeech(this, TextToSpeech.OnInitListener { status: Int ->
			if (status == TextToSpeech.SUCCESS) speakTimeSmartMethod.tts = tts
			else Toast.makeText(this@MainActivity, "Failed to init TTS. Status code: $status", Toast.LENGTH_SHORT).show()
			tts = null
		})
	}

	private fun getTime() {
		if (getTimeAsyncTask != null) return

		getTimeAsyncTask = object: GetTimeAsyncTask() {
			override fun onPostExecute(result: Result<Date, Exception>) {
				getTimeAsyncTask = null

				result.fold(
						{ value -> txtTime.text = "Time: $value"},
						{ error -> txtTime.text = "Failed to get time. Error: $error" })

				speakTimeSmartMethod.date = result.component1()
			}
		}
		getTimeAsyncTask?.execute()
	}
}
