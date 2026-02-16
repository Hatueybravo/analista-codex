package com.analista.fileorganizer.data.local

import androidx.room.TypeConverter
import com.analista.fileorganizer.data.model.*

class Converters {

    @TypeConverter
    fun fromFileCategory(value: FileCategory): String = value.name

    @TypeConverter
    fun toFileCategory(value: String): FileCategory = FileCategory.valueOf(value)

    @TypeConverter
    fun fromAgentAction(value: AgentAction): String = value.name

    @TypeConverter
    fun toAgentAction(value: String): AgentAction = AgentAction.valueOf(value)

    @TypeConverter
    fun fromCommandStatus(value: CommandStatus): String = value.name

    @TypeConverter
    fun toCommandStatus(value: String): CommandStatus = CommandStatus.valueOf(value)

    @TypeConverter
    fun fromCommandSource(value: CommandSource): String = value.name

    @TypeConverter
    fun toCommandSource(value: String): CommandSource = CommandSource.valueOf(value)
}
