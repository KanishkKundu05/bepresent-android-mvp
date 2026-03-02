package com.bepresent.android.di

import android.content.Context
import androidx.room.Room
import com.bepresent.android.data.db.AppIntentionDao
import com.bepresent.android.data.db.BePresentDatabase
import com.bepresent.android.data.db.PresentSessionDao
import com.bepresent.android.data.db.ScheduledSessionDao
import com.bepresent.android.data.db.SyncQueueDao
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
    fun provideDatabase(@ApplicationContext context: Context): BePresentDatabase {
        return Room.databaseBuilder(
            context,
            BePresentDatabase::class.java,
            "bepresent.db"
        )
            .addMigrations(BePresentDatabase.MIGRATION_1_2, BePresentDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideAppIntentionDao(database: BePresentDatabase): AppIntentionDao {
        return database.appIntentionDao()
    }

    @Provides
    fun providePresentSessionDao(database: BePresentDatabase): PresentSessionDao {
        return database.presentSessionDao()
    }

    @Provides
    fun provideSyncQueueDao(database: BePresentDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }

    @Provides
    fun provideScheduledSessionDao(database: BePresentDatabase): ScheduledSessionDao {
        return database.scheduledSessionDao()
    }
}
