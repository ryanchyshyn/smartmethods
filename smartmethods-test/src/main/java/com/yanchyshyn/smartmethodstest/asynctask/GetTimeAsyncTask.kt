package com.yanchyshyn.smartmethodstest.asynctask

import android.os.AsyncTask
import com.github.kittinunf.result.Result
import com.instacart.library.truetime.TrueTime
import java.util.*

open class GetTimeAsyncTask : AsyncTask<Void, Void, Result<Date, Exception>>() {
	override fun doInBackground(vararg params: Void?): Result<Date, Exception> {
		return Result.of{
			TrueTime.build().withServerResponseDelayMax(1000).initialize()
			TrueTime.now()
		}
	}
}
