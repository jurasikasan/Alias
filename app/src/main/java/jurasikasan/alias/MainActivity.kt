package jurasikasan.alias

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import jurasikasan.alias.ui.main.MainFragment
import jurasikasan.alias.ui.main.MainViewModel

class MainActivity : AppCompatActivityStreamMusic() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.teamNames = intent.getStringArrayListExtra(EXTRA_TEAMS_NAMES)
        viewModel.teamList = ArrayList(viewModel.teamNames!!.size)
        for (name in viewModel.teamNames!!) {
            viewModel.teamList!!.add(Team(name))
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }

    class LeaveGameDialogFragment(
        var okButtonListener: DialogInterface.OnClickListener
    ) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                AlertDialog.Builder(it)
                    .setTitle(resources.getString(R.string.leave_game_question))
                    .setMessage(resources.getString(R.string.leave_game_warning))
                    .setPositiveButton(resources.getString(R.string.ok), okButtonListener)
                    .setNegativeButton(resources.getString(R.string.cancel), null)
                    .create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }

    override fun onBackPressed() {
        LeaveGameDialogFragment { _, _ ->
            super.onBackPressed()
        }.show(supportFragmentManager, "confirm_leave_game")
    }
}
