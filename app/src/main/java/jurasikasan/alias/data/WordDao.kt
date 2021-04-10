package jurasikasan.alias.data

import androidx.room.*
import java.util.*

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE lastPlayed isnull OR  lastPlayed < :date ORDER BY RANDOM() LIMIT 1")
    fun getNextWord(date: Date): Word?

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT 1")
    fun getNextWord(): Word

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<Word>)

    @Update
    fun updateWords(vararg words: Word)
}
