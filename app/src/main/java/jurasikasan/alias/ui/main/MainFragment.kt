package jurasikasan.alias.ui.main

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableLong
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.card.MaterialCardView
import jurasikasan.alias.*
import jurasikasan.alias.data.AppDatabase
import jurasikasan.alias.data.Word
import jurasikasan.alias.databinding.MainFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

const val multiplier = 60
const val roundTime: Long = multiplier * 1000L
const val winWordsAmount: Int = multiplier

class MainFragment : Fragment() {
    private var wordComplexityMin: Int = 0
    private var wordComplexityMax: Int = 0
    private var ignorePlayedInterval: Long = 0

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var binding: MainFragmentBinding
    private lateinit var viewModel: MainViewModel
    var playerEndTime: MediaPlayer? = null
    var playerSuccess: MediaPlayer? = null
    var playerFail: MediaPlayer? = null
    var db: AppDatabase? = null

    private var roundDeadLine: Long? = null

    class CommonWordDialogFragment(
        var word: String,
        var success: Boolean,
        var teamNames: ArrayList<String>,
        var listener: DialogInterface.OnClickListener,
        var cancelListener: DialogInterface.OnClickListener
    ) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val cs: Array<CharSequence> =
                teamNames.toArray(arrayOfNulls<CharSequence>(teamNames.size))

            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle(word + " " + (if (success) "⇧" else "⇩") + " Pick the team") // TODO localization
                    .setItems(
                        cs,
                        listener
                    )
                    .setNegativeButton(
                        "No one", cancelListener // TODO localization
                    )
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        playerEndTime = MediaPlayer.create(activity, R.raw.time_over)
        playerSuccess = MediaPlayer.create(activity, R.raw.success)
        playerFail = MediaPlayer.create(activity, R.raw.fail)
        db = AppDatabase.getInstance(activity!!)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        val mAdapter = TeamProgressAdapter(viewModel.teamList!!)
        val mLayoutManager = LinearLayoutManager(activity!!.applicationContext)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.teams.layoutManager = mLayoutManager
        binding.teams.itemAnimator = DefaultItemAnimator()
        binding.teams.adapter = mAdapter

        val swipeDismissBehavior = jurasikasan.alias.SwipeDismissBehavior<View>()
        val cardContentLayout: MaterialCardView = binding.cardContentLayout
        val coordinatorParams = cardContentLayout.layoutParams as CoordinatorLayout.LayoutParams
        coordinatorParams.behavior = swipeDismissBehavior
        swipeDismissBehavior.listener =
            object : jurasikasan.alias.SwipeDismissBehavior.OnDismissListener {
                override fun onDismissUp(view: View?) {
                    playerSuccess!!.start()
                    val timeLeft = roundDeadLine!!.minus(Date().time)
                    when {
                        timeLeft > 50 -> { // OK, next
                            GlobalScope.launch(Dispatchers.Main) {
                                endTurn(true, binding.currentRound!!.ownerTeam)
                                startTurn()
                            }
                        }
                        timeLeft > 0 -> { // last, but own (just in time)
                            GlobalScope.launch(Dispatchers.Main) {
                                endTurn(true, binding.currentRound!!.ownerTeam)
                                endRound()
                            }
                        }
                        else -> { // common word
                            showCommonSuccess()
                        }
                    }
                }

                override fun onDismissDown(view: View?) {
                    playerFail!!.start()
                    val timeLeft = roundDeadLine!!.minus(Date().time)
                    when {
                        timeLeft > 50 -> { // OK, next
                            GlobalScope.launch(Dispatchers.Main) {
                                endTurn(false, binding.currentRound!!.ownerTeam)
                                startTurn()
                            }
                        }
                        timeLeft > 0 -> { // last, but own (just in time)
                            GlobalScope.launch(Dispatchers.Main) {
                                endTurn(false, binding.currentRound!!.ownerTeam)
                                endRound()
                            }
                        }
                        else -> { // common word
                            showCommonFail()
                        }
                    }

                }


                override fun onDragStateChanged(state: Int) {
                    when (state) {
                        SwipeDismissBehavior.STATE_DRAGGING, SwipeDismissBehavior.STATE_SETTLING -> cardContentLayout.isDragged =
                            true
                        SwipeDismissBehavior.STATE_IDLE -> cardContentLayout.isDragged =
                            false
                        else -> {
                        }
                    }
                }
            }
        prepareRound(viewModel.currentTeamIndex)
        binding.ready.setOnClickListener { startRound() }


        binding.showHistory.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.container, HistoryFragment.newInstance())
                .commitNow()
        }

        binding.finish.setOnClickListener {
            startActivity(Intent(activity, NewGameSettingsActivity::class.java).apply { })
        }
        val sharedPref = requireActivity().getSharedPreferences("alias", Context.MODE_PRIVATE)
        ignorePlayedInterval =
            sharedPref.getFloat("playedWordsIgnoreDays", 0f).toLong() * MILLISECONDS_IN_DAY
        wordComplexityMin = sharedPref.getFloat("wordComplexityMin", 0f).toInt()
        wordComplexityMax = sharedPref.getFloat("wordComplexityMax", 0f).toInt()
    }

    private fun resetCard(cardContentLayout: MaterialCardView) {
        val params = cardContentLayout
            .layoutParams as CoordinatorLayout.LayoutParams
        params.setMargins(0, 0, 0, 0)
        cardContentLayout.alpha = 1.0f
        cardContentLayout.requestLayout()
    }

    private suspend fun startTurn() {
        val word = getWord()
        binding.currentTurn = Round.Turn(word)
        resetCard(binding.cardContentLayout)
    }

    private suspend fun getWord() = withContext(Dispatchers.Default) {
        var word = db!!.wordDao().getNextWord(
            Date(Date().time - ignorePlayedInterval),
            wordComplexityMin,
            wordComplexityMax
        )
        if (word == null) {
            word = db!!.wordDao().getNextWord()
        }
        return@withContext word
    }

    private suspend fun updateWord(word: Word) = withContext(Dispatchers.Default) {
        word.lastPlayed = Date()
        db!!.wordDao().updateWords(word)
    }

    private suspend fun endTurn(success: Boolean, team: Int?) {
        binding.currentTurn!!.success = success
        binding.currentTurn!!.team = team
        binding.currentRound!!.turns.add(binding.currentTurn!!)

        if (team == null) {
            return
        }
        if (success) {
            binding.correct!!.set(binding.correct!!.get() + 1)
            viewModel.teamList!![team].scores++
            binding.teams.adapter!!.notifyDataSetChanged()
        } else {
            binding.wrong!!.set(binding.wrong!!.get() + 1)
            viewModel.teamList!![team].scores--
            binding.teams.adapter!!.notifyDataSetChanged()
        }
        updateWord(binding.currentTurn!!.word)
    }

    private fun showCommonSuccess() {
        CommonWordDialogFragment(
            binding.currentTurn!!.word.value, true, viewModel.teamNames!!,
            DialogInterface.OnClickListener { _, which ->
                GlobalScope.launch(Dispatchers.Main) {
                    endTurn(true, which)
                    endRound()
                }
            },
            DialogInterface.OnClickListener { _, _ ->
                GlobalScope.launch(Dispatchers.Main) {
                    endTurn(true, null)
                    endRound()
                }
            }
        ).show(activity!!.supportFragmentManager, "commonWord")
    }

    private fun showCommonFail() {
        val currentTeamAsList: ArrayList<String> = ArrayList<String>()
        currentTeamAsList.add(viewModel.teamNames!![binding.currentRound!!.ownerTeam])

        CommonWordDialogFragment(
            binding.currentTurn!!.word.value, false,
            currentTeamAsList,
            DialogInterface.OnClickListener { _, _ ->
                GlobalScope.launch(Dispatchers.Main) {
                    endTurn(false, binding.currentRound!!.ownerTeam)
                    endRound()
                }
            },
            DialogInterface.OnClickListener { _, _ ->
                GlobalScope.launch(Dispatchers.Main) {
                    endTurn(false, null)
                    endRound()
                }
            }
        ).show(activity!!.supportFragmentManager, "commonWord")
    }


    private fun prepareRound(team: Int) {
        binding.teams.smoothScrollToPosition(team)
        binding.timeLeft = ObservableLong(roundTime)
        binding.currentRound = Round(team)

        binding.correct = ObservableInt(0)
        binding.wrong = ObservableInt(0)

        binding.roundInProgress = false
        binding.currentTurn = null
    }

    private fun startRound() {
        binding.teams.smoothScrollToPosition(viewModel.currentTeamIndex)
        roundDeadLine = Date().time + roundTime
        var signal = false
        val countDown = object : CountDownTimer(roundTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.timeLeft!!.set(millisUntilFinished)
                if (millisUntilFinished in 1000..2500 && !signal) {
                    playerEndTime!!.start()
                    signal = true
                }
            }

            override fun onFinish() {
                cancel()
                playerEndTime!!.start()
            }
        }
        binding.roundInProgress = true
        GlobalScope.launch(Dispatchers.Main) {
            startTurn()
        }
        countDown.start()
    }


    fun endRound() {
        viewModel.gameLog.add(binding.currentRound!!)
        viewModel.currentTeamIndex++
        if (viewModel.currentTeamIndex == viewModel.teamList!!.size) {
            viewModel.currentTeamIndex = 0
            checkWinner()
        }
        prepareRound(viewModel.currentTeamIndex)
    }

    class WinnerDialogFragment(var winner: Team) : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle("Winner")
                    .setMessage(winner.name + " (" + winner.scores + ")")
                    .setPositiveButton(
                        resources.getString(R.string.ok),
                        null
                    ).setCancelable(false)
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }

    private fun checkWinner() {
        binding.winner = null
        var best: Team = viewModel.teamList!![0]

        for (team in viewModel.teamList!!) {
            if (team.scores >= best.scores) {
                best = team
            }
        }
        if (best.scores >= winWordsAmount) {
            val winners: ArrayList<Team> = ArrayList()

            for (team in viewModel.teamList!!) {
                if (team.scores == best.scores) {
                    winners.add(team)
                }
            }

            if (winners.size == 1) {
                binding.winner = winners[0]
                WinnerDialogFragment(winners[0]).show(activity!!.supportFragmentManager, "winner")
            }
        }
    }

}