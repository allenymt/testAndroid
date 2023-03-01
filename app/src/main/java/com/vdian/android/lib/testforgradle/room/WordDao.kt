package com.vdian.android.lib.testforgradle.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * @author yulun
 * @since 2022-07-25 16:17
 */

@Dao
interface WordDao {
    //插入多个数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: MutableList<WordEntity>)

    //
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(words: WordEntity)

    //获取所有数据
    @Query("SELECT * FROM word_table")
    fun queryAll(): MutableList<WordEntity>

    //根据id获取一个数据
    @Query("SELECT * FROM word_table WHERE id = :id")
    fun getWordById(id: Int): WordEntity?

    //删除表中所有数据
    @Query("DELETE FROM word_table")
    suspend fun deleteAll()

    //通过id修改数据
    @Query("UPDATE word_table SET word=:word WHERE id=:id")
    suspend fun updateData(id: Long, word: String)

    //根据Id删除数据
    @Query("DELETE FROM word_table WHERE id=:id")
    suspend fun deleteById(id: Long)

    //根据属性值删除数据
    @Query("DELETE FROM word_table WHERE word=:word")
    suspend fun deleteByName(word: String)

}