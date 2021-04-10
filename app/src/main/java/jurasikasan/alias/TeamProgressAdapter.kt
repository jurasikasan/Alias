package jurasikasan.alias

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class TeamProgressAdapter(private val teams: List<Team>) :
    RecyclerView.Adapter<TeamProgressAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.title)
        var scores: TextView = view.findViewById(R.id.scores)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.team_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val team: Team = teams[position]
        holder.title.text = team.name
        holder.scores.text = "${team.scores}"
    }

    override fun getItemCount(): Int {
        return teams.size
    }
}