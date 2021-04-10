package jurasikasan.alias.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import jurasikasan.alias.HistoryListAdapter
import jurasikasan.alias.R
import jurasikasan.alias.databinding.HistoryFragmentBinding


class HistoryFragment : Fragment() {
    companion object {
        fun newInstance() = HistoryFragment()
    }

    private lateinit var binding: HistoryFragmentBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HistoryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        val historyAdapter = HistoryListAdapter(
            activity!!,
            activity!!.supportFragmentManager,
            viewModel.gameLog.toTypedArray(),
            viewModel.teamNames
        )

        binding.rounds.setAdapter(historyAdapter)
        if (viewModel.gameLog.size > 0)
            binding.rounds.expandGroup(viewModel.gameLog.size - 1)

        binding.play.setOnClickListener {
            for (team in viewModel.teamList!!) {
                team.scores = 0
            }
            for (round in viewModel.gameLog) {
                for (turn in round.turns) {
                    if (turn.team == null) continue
                    if (turn.success) {
                        viewModel.teamList!![turn.team!!].scores++
                    } else {
                        viewModel.teamList!![turn.team!!].scores--
                    }
                }
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }
}