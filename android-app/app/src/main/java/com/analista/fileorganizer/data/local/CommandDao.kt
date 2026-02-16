package com.analista.fileorganizer.data.local

import androidx.room.*
import com.analista.fileorganizer.data.model.AgentCommand
import com.analista.fileorganizer.data.model.CommandStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {

    @Query("SELECT * FROM agent_commands ORDER BY createdAt DESC")
    fun getAllCommands(): Flow<List<AgentCommand>>

    @Query("SELECT * FROM agent_commands WHERE status = :status ORDER BY createdAt ASC")
    fun getCommandsByStatus(status: CommandStatus): Flow<List<AgentCommand>>

    @Query("SELECT * FROM agent_commands ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentCommands(limit: Int = 50): Flow<List<AgentCommand>>

    @Insert
    suspend fun insertCommand(command: AgentCommand): Long

    @Update
    suspend fun updateCommand(command: AgentCommand)

    @Query("DELETE FROM agent_commands WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
