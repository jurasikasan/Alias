package jurasikasan.alias

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import jurasikasan.alias.data.Word

class HistoryListAdapter internal constructor(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val rounds: Array<Round>,
    private val teams: ArrayList<String>?
) : BaseExpandableListAdapter() {

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return this.rounds[listPosition].turns[expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    class InfoDialogFragment(
        var word: Word
    ) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle(word.value)
                    .setMessage(word.description)
                    .setPositiveButton(
                        resources.getString(R.string.ok), null
                    )
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }

    class PickTeamDialogFragment(
        var word: String,
        var teamNames: ArrayList<String>,
        var listener: DialogInterface.OnClickListener,
        var cancelListener: DialogInterface.OnClickListener
    ) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val cs: Array<CharSequence> =
                teamNames.toArray(arrayOfNulls<CharSequence>(teamNames.size))

            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle("'$word' \n${resources.getString(R.string.pick_team)}")
                    .setItems(
                        cs,
                        listener
                    )
                    .setNegativeButton(
                        resources.getString(R.string.no_one), cancelListener
                    )
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }


    override fun getChildView(
        listPosition: Int,
        expandedListPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var convertView = convertView
        val turn = getChild(listPosition, expandedListPosition) as Round.Turn
        if (convertView == null) {
            val layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.item_turn_history, null)
        }
        val teamView = convertView!!.findViewById<TextView>(R.id.team)
        teamView.text =
            if (turn.team == null) parent.resources.getString(R.string.no_one)
            else "${parent.resources.getString(R.string.team)} #${turn.team!! + 1}"

        teamView.setOnClickListener {
            PickTeamDialogFragment(
                turn.word.value,
                teams!!,
                DialogInterface.OnClickListener { _, which ->
                    turn.team = which
                    teamView.text = teams[which]
                    teamView.invalidate()
                },
                DialogInterface.OnClickListener { _, _ ->
                    turn.team = null
                    teamView.text = parent.resources.getString(R.string.no_one)
                    teamView.invalidate()
                }
            ).show(fragmentManager, "pickTeam")
        }

        val wordView = convertView.findViewById<TextView>(R.id.word)
        wordView.text = turn.word.value
        wordView.setOnClickListener {
            InfoDialogFragment(turn.word).show(fragmentManager, "info")
        }

        val successView = convertView.findViewById<CheckBox>(R.id.success)
        successView.isChecked = turn.success
        successView.setOnClickListener {
            turn.success = (it as CheckBox).isChecked
        }
        return convertView
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.rounds[listPosition].turns.size
    }

    override fun getGroup(listPosition: Int): Any {
        return this.rounds[listPosition]
    }

    override fun getGroupCount(): Int {
        return this.rounds.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getGroupView(
        listPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var convertView = convertView
        val listTitle = "${parent.resources.getString(R.string.round)} ${listPosition + 1}"
        if (convertView == null) {
            val layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.item_round_history, null)
        }
        val listTitleTextView = convertView!!.findViewById<TextView>(R.id.title)
        listTitleTextView.setTypeface(null, Typeface.BOLD)
        listTitleTextView.text = listTitle
        return convertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }
}