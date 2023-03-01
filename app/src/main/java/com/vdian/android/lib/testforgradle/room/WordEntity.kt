package com.vdian.android.lib.testforgradle.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author yulun
 * @since 2022-07-25 15:56
 */
@Entity(tableName = "word_table")
data class WordEntity(

    // 修饰某一列
    @ColumnInfo(name = "word")
    val word:String?,

    // PrimaryKey修饰的是主键
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 新增升级字段
    @ColumnInfo(name = "content")
    val content: String?
)