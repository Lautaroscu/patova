package ar.com.patova.data.local

import android.content.Context
import androidx.room.Room
import ar.com.patova.data.local.daos.BlacklistDao
import ar.com.patova.data.local.daos.LocalPreferencesDao
import ar.com.patova.data.local.daos.WhitelistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PatovaDatabase {
        return Room.databaseBuilder(
            context,
            PatovaDatabase::class.java,
            "patova_db"
        )
            .addMigrations(PatovaDatabase.MIGRATION_1_2, PatovaDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideCachedValidationDao(db: PatovaDatabase) = db.cachedValidationDao()

    @Provides
    fun provideCallEventDao(db: PatovaDatabase) = db.callEventDao()

    @Provides
    fun providePendingReportDao(db: PatovaDatabase) = db.pendingReportDao()

    @Provides
    fun provideLocalPreferencesDao(db: PatovaDatabase): LocalPreferencesDao = db.localPreferencesDao()

    @Provides
    fun provideWhitelistDao(db: PatovaDatabase): WhitelistDao = db.whitelistDao()

    @Provides
    fun provideBlacklistDao(db: PatovaDatabase): BlacklistDao = db.blacklistDao()
}
