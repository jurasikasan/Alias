package jurasikasan.alias

import android.media.AudioManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class AppCompatActivityStreamMusic : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
    }
}
