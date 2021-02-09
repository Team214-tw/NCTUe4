package com.team214.nctue4.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AnnDao {
    @Query("SELECT * FROM annTable ORDER BY date DESC")
    fun getAll(): List<AnnItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg user: AnnItem)

    @Query("DELETE FROM annTable")
    fun deleteAll()

    @Query("DELETE FROM annTable WHERE e3type = 0")
    fun deleteAllNewE3()

}