package jurasikasan.alias.ui.main

import androidx.lifecycle.ViewModel
import jurasikasan.alias.Round
import jurasikasan.alias.Team

class MainViewModel : ViewModel() {
    var teamNames: ArrayList<String>? = null
    var teamList: ArrayList<Team>? = null
    var gameLog: ArrayList<Round> = ArrayList()
    var currentTeamIndex: Int = 0
}