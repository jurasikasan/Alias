package jurasikasan.alias.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val value: String,
    val description: String,
    var lastPlayed: Date?,
) {
    override fun toString() = value
}
