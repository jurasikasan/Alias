package jurasikasan.alias

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import jurasikasan.alias.data.AppDatabase
import jurasikasan.alias.databinding.ActivityNewGameSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

const val EXTRA_PLAYERS = "jurasikasan.alias.settings.players"
const val EXTRA_TEAMS = "jurasikasan.alias.settings.teams"

const val MIN_PLAYERS_IN_TEAM = 2

const val MIN_TEAMS = 2
const val MAX_TEAMS = 5

const val MIN_PLAYERS = MIN_TEAMS * MIN_PLAYERS_IN_TEAM
const val MAX_PLAYERS = MAX_TEAMS * MIN_PLAYERS_IN_TEAM

const val MILLISECONDS_IN_DAY: Long = 24 * 60 * 60 * 1000


class NewGameSettingsActivity : AppCompatActivityStreamMusic() {
    private lateinit var binding: ActivityNewGameSettingsBinding
    private lateinit var db: AppDatabase
    private lateinit var sharedPreferences: SharedPreferences

    override fun onResume() {
        super.onResume()
        GlobalScope.launch(Dispatchers.Default) {
            binding.wordsMatches = ObservableInt(getMatchingWordsCount())
        }
    }

    private fun getMatchingWordsCount(): Int {
        return db.wordDao().countWords(
            Date(
                Date().time - binding.getPlayedWordsIgnoreDays()!!
                    .get().toLong() * MILLISECONDS_IN_DAY
            ),
            binding.wordComplexityRange.values[0].toInt(),
            binding.wordComplexityRange.values[1].toInt()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getInstance(this)
        binding = DataBindingUtil.setContentView(
            this, R.layout.activity_new_game_settings
        )
        sharedPreferences = getSharedPreferences("alias", Context.MODE_PRIVATE)

        binding.playersAmount.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (MIN_PLAYERS..MAX_PLAYERS).toMutableList()
        )

        binding.teamsAmount.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (MIN_TEAMS..MAX_TEAMS).toMutableList()
        )

        class UsersCountSpinnerSelectedListener : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val playersSelected = parent.getItemAtPosition(pos) as Int
                val teamsUserSelectedPosition = binding.teamsAmount.selectedItemPosition

                val adapter = binding.teamsAmount.adapter as ArrayAdapter<Int>
                adapter.clear()

                val maxTeams = playersSelected / MIN_PLAYERS_IN_TEAM
                val teamsOptions = (MIN_TEAMS..maxTeams).toList()
                adapter.addAll(teamsOptions)

                if (teamsOptions.size > teamsUserSelectedPosition) {
                    binding.teamsAmount.setSelection(teamsUserSelectedPosition)
                } else {
                    binding.teamsAmount.setSelection(0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // ignored
            }
        }

        binding.playersAmount.onItemSelectedListener = UsersCountSpinnerSelectedListener()

        binding.start.setOnClickListener {
            startActivity(
                Intent(this, TossUpActivity::class.java).apply {
                    putExtra(EXTRA_PLAYERS, binding.playersAmount.selectedItem as Int)
                    putExtra(EXTRA_TEAMS, binding.teamsAmount.selectedItem as Int)
                }
            )
        }

        binding.goToSettings.setOnClickListener {
            startActivity(
                Intent(this, SettingsActivity::class.java)
            )
        }

        binding.setPlayedWordsIgnoreDays(
            ObservableFloat(
                sharedPreferences.getFloat("playedWordsIgnoreDays", 30f)
            )
        )
        binding.playedWordsIgnoreDays.value = binding.getPlayedWordsIgnoreDays()!!.get()
        binding.playedWordsIgnoreDays.addOnChangeListener { _, value, _ ->
            with(sharedPreferences.edit()) {
                putFloat("playedWordsIgnoreDays", value)
                apply()
            }
            binding.getPlayedWordsIgnoreDays()!!.set(value)
            GlobalScope.launch(Dispatchers.Default) {
                binding.wordsMatches!!.set(getMatchingWordsCount())
            }
        }

        val complexityMin = sharedPreferences.getFloat("wordComplexityMin", 0f)
        val complexityMax = sharedPreferences.getFloat("wordComplexityMax", 100f)
        binding.wordComplexityRange.values = listOf(complexityMin, complexityMax)
        binding.wordComplexityRange.addOnChangeListener { _, _, _ ->
            with(sharedPreferences.edit()) {
                putFloat("wordComplexityMin", binding.wordComplexityRange.values[0])
                putFloat("wordComplexityMax", binding.wordComplexityRange.values[1])
                apply()
            }
            GlobalScope.launch(Dispatchers.Default) {
                binding.wordsMatches!!.set(getMatchingWordsCount())
            }
        }
        GlobalScope.launch(Dispatchers.Default) {
            binding.wordsMatches = ObservableInt(getMatchingWordsCount())
        }
    }
}
