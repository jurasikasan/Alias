package jurasikasan.alias.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.coroutineScope
import com.google.gson.stream.JsonReader
import jurasikasan.alias.data.AppDatabase
import jurasikasan.alias.data.Word


const val DATA_FILENAME = "words.json"

class SeedDatabaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = coroutineScope {
        try {
            applicationContext.assets.open(DATA_FILENAME).use { inputStream ->
                JsonReader(inputStream.reader()).use { jsonReader ->
                    val wordType = object : TypeToken<List<Word>>() {}.type
                    val wordList: List<Word> = Gson().fromJson(jsonReader, wordType)
                    val database = AppDatabase.getInstance(applicationContext)
                    database.wordDao().insertAll(wordList)
                    Result.success()
                }
            }

        } catch (ex: Exception) {
            Log.e(TAG, "Error seeding database", ex)
            Result.failure()
        }

    }

    companion object {
        private const val TAG = "SeedDatabaseWorker"
    }
}
