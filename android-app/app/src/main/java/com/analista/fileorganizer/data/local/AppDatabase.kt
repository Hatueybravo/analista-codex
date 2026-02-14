package com.analista.fileorganizer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.analista.fileorganizer.data.model.AgentCommand
import com.analista.fileorganizer.data.model.FileItem

@Database(
    entities = [FileItem::class, AgentCommand::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun commandDao(): CommandDao
}
