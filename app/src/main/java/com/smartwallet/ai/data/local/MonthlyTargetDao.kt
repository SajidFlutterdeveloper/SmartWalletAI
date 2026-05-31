package com.smartwallet.ai.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smartwallet.ai.data.model.MonthlyTarget

@Dao
interface MonthlyTargetDao {
    @Query("SELECT * FROM monthly_targets WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun getAllTargets(userId: String): LiveData<List<MonthlyTarget>>

    @Query("SELECT * FROM monthly_targets WHERE userId = :userId AND month = :month AND year = :year")
    suspend fun getTarget(userId: String, month: Int, year: Int): MonthlyTarget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: MonthlyTarget)

    @Update
    suspend fun updateTarget(target: MonthlyTarget)
}
