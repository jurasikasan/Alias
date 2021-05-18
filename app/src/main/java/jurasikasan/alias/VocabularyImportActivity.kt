package jurasikasan.alias

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import jurasikasan.alias.data.AppDatabase
import jurasikasan.alias.data.Word
import jurasikasan.alias.databinding.ActivityVocabularyImportBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val REQUEST_FILE_GET = 1

class VocabularyImportActivity : AppCompatActivity() {
    lateinit var binding: ActivityVocabularyImportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this, R.layout.activity_vocabulary_import
        )
        selectFile()
    }

    private fun selectFile() {
        val intent = Intent()
        intent.type = "*/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select file"),
            REQUEST_FILE_GET
        )
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int) {
        super.startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE_GET && resultCode == Activity.RESULT_OK && data != null) {
            val db = AppDatabase.getInstance(this)
            val uri = data.data!!
            GlobalScope.launch(Dispatchers.Main) {
                import(db, uri)
            }
        }
        onBackPressed()
    }

    private suspend fun import(db: AppDatabase, uri: Uri) {
        binding.message.text = getString(R.string.read_file)
        val words = readAllWords(uri)
        binding.message.text = getString(R.string.write_db)
        writeData(words, db)
        binding.message.text = getString(R.string.done)
    }

    private suspend fun readAllWords(uri: Uri) = withContext(Dispatchers.Default) {
        val inputStream = contentResolver.openInputStream(uri)
        JsonReader(inputStream!!.reader()).use { jsonReader ->
            val wordType = object : TypeToken<List<Word>>() {}.type
            return@withContext Gson().fromJson<List<Word>?>(jsonReader, wordType)
        }
    }

    private suspend fun writeData(data: List<Word>, db: AppDatabase) =
        withContext(Dispatchers.Default) {
            db.wordDao().deleteAll()
            db.wordDao().insertAll(data)
        }
}
