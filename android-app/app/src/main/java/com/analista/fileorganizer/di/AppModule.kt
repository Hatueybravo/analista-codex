package com.analista.fileorganizer.di

import android.content.Context
import androidx.room.Room
import com.analista.fileorganizer.data.local.AppDatabase
import com.analista.fileorganizer.data.local.CommandDao
import com.analista.fileorganizer.data.local.FileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "file_organizer.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFileDao(database: AppDatabase): FileDao = database.fileDao()

    @Provides
    fun provideCommandDao(database: AppDatabase): CommandDao = database.commandDao()
}
