package jurasikasan.alias.data

import androidx.room.*
import java.util.*

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE (lastPlayed isnull OR  lastPlayed < :date) AND complexity BETWEEN :complexityMin AND :complexityMax ORDER BY RANDOM() LIMIT 1")
    fun getNextWord(date: Date, complexityMin: Int, complexityMax: Int): Word?

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT 1")
    fun getNextWord(): Word

    @Query("SELECT * FROM words")
    fun getAllWords(): List<Word>

    @Query("SELECT COUNT(id) FROM words WHERE (lastPlayed isnull OR  lastPlayed < :date) AND complexity BETWEEN :complexityMin AND :complexityMax")
    fun countWords(date: Date, complexityMin: Int, complexityMax: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<Word>)

    @Update
    fun updateWords(vararg words: Word)

    @Query("DELETE FROM words")
    fun deleteAll()

}
