package jurasikasan.alias

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonWriter
import jurasikasan.alias.data.AppDatabase
import jurasikasan.alias.data.Word
import jurasikasan.alias.databinding.ActivityVocabularyExportBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

const val REQUEST_FILE_CREATE = 2
const val patternDate = "dd-MM-yyyy"

class VocabularyExportActivity : AppCompatActivity() {
    lateinit var binding: ActivityVocabularyExportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this, R.layout.activity_vocabulary_export
        )
        createFile()
    }

    private fun createFile() {
        val simpleDateFormat = SimpleDateFormat(patternDate)
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, "alias_${simpleDateFormat.format(Date())}.json")
        }
        startActivityForResult(intent, REQUEST_FILE_CREATE)
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int) {
        super.startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE_CREATE && resultCode == Activity.RESULT_OK && data != null) {
            val db = AppDatabase.getInstance(this)
            val uri = data.data!!
            GlobalScope.launch(Dispatchers.Main) {
                export(db, uri)
            }
        }
        onBackPressed()
    }

    private suspend fun export(db: AppDatabase, uri: Uri) {
        binding.message.text = getString(R.string.read_db)
        val words = readAllWords(db)
        binding.message.text = getString(R.string.write_file)
        writeData(words, uri)
        binding.message.text = getString(R.string.done)
    }

    private suspend fun readAllWords(db: AppDatabase) = withContext(Dispatchers.Default) {
        db.wordDao().getAllWords()
    }

    private suspend fun writeData(data: List<Word>, uri: Uri) = withContext(Dispatchers.Default) {
        contentResolver.openFileDescriptor(uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { outputStream ->
                JsonWriter(outputStream.writer()).use { jsonWriter ->
                    val wordType = object : TypeToken<List<Word>>() {}.type
                    Gson().toJson(data, wordType, jsonWriter)
                }
            }
        }
    }
}
