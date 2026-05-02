package ar.com.numguard.data.local

import android.content.Context
import androidx.room.Room
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
    fun provideNumGuardDatabase(@ApplicationContext context: Context): NumGuardDatabase =
        Room.databaseBuilder(
            context,
            NumGuardDatabase::class.java,
            "numguard.db"
        )
            .addMigrations(NumGuardDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideCachedValidationDao(db: NumGuardDatabase): CachedValidationDao =
        db.cachedValidationDao()

    @Provides
    @Singleton
    fun provideCallEventDao(db: NumGuardDatabase): CallEventDao =
        db.callEventDao()

    @Provides
    @Singleton
    fun providePendingReportDao(db: NumGuardDatabase): PendingReportDao =
        db.pendingReportDao()
}
