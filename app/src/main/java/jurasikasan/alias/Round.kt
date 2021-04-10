package jurasikasan.alias

import jurasikasan.alias.data.Word

class Round(val ownerTeam: Int) {

    class Turn(var word: Word) {
        var success: Boolean = false
        var team: Int? = null
    }

    val turns: ArrayList<Turn> = ArrayList()
}
