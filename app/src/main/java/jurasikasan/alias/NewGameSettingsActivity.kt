package jurasikasan.alias

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import jurasikasan.alias.data.AppDatabase
import jurasikasan.alias.databinding.ActivityNewGameSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val EXTRA_PLAYERS = "jurasikasan.alias.settings.players"
const val EXTRA_TEAMS = "jurasikasan.alias.settings.teams"

const val MIN_PLAYERS_IN_TEAM = 2

const val MIN_TEAMS = 2
const val MAX_TEAMS = 5

const val MIN_PLAYERS = MIN_TEAMS * MIN_PLAYERS_IN_TEAM
const val MAX_PLAYERS = MAX_TEAMS * MIN_PLAYERS_IN_TEAM

class NewGameSettingsActivity : AppCompatActivityStreamMusic() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(this) // to init default dictionary load
        GlobalScope.launch(Dispatchers.Default) {
            db.wordDao().getNextWord()
        }
        val binding: ActivityNewGameSettingsBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_new_game_settings
        )

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

        binding.goToSettings.setOnClickListener{
            startActivity(
                Intent(this, SettingsActivity::class.java)
            )
        }
    }
}
