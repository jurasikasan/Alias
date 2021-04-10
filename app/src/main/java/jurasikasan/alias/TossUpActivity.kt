package jurasikasan.alias

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import jurasikasan.alias.databinding.ActivityTossUpBinding
import kotlin.random.Random

const val EXTRA_TEAMS_NAMES = "jurasikasan.alias.settings.teams.names"

class TossUpActivity : AppCompatActivityStreamMusic() {
    private fun prepareTickets(playersCount: Int, teamsCount: Int): ArrayList<Int> {
        val tickets = ArrayList<Int>(playersCount)
        var playersLeft = playersCount

        while (true) {
            for (i in 0 until teamsCount) {
                tickets.add(i)
                playersLeft--
                if (playersLeft == 0) {
                    return tickets
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityTossUpBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_toss_up
        )

        val playersCount = intent.getIntExtra(EXTRA_PLAYERS, 0)
        val teamsCount = intent.getIntExtra(EXTRA_TEAMS, 0)

        val teams = ArrayList<Team>(teamsCount)
        for (i in 0 until teamsCount) {
            teams.add(Team("${resources.getString(R.string.team)} #${i + 1}"))
        }

        val tickets = prepareTickets(playersCount, teamsCount)

        for (i in 0 until playersCount) {
            val ticketsLeft = playersCount - i
            val chosenIndex = Random.nextInt(
                0, // [from] (inclusive)
                ticketsLeft // [until] (exclusive)
            )
            val teamNumber = tickets[chosenIndex]
            tickets.remove(teamNumber)
            teams[teamNumber].players.add("${resources.getString(R.string.player)} ${i + 1}")
        }

        binding.teams.setAdapter(
            TeamPlayersListAdapter(
                this,
                teams.toTypedArray()
            )
        )

        for (i in 0 until teamsCount)
            binding.teams.expandGroup(i)

        binding.play.setOnClickListener {
            val teamNames = ArrayList<String>(teamsCount)

            for (team in teams) {
                teamNames.add(team.name)
            }

            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    putStringArrayListExtra(EXTRA_TEAMS_NAMES, teamNames)
                }
            )
        }
    }
}
