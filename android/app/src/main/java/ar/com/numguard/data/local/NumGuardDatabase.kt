package ar.com.numguard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedValidationEntity::class,
        CallEventEntity::class,
        PendingReportEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class NumGuardDatabase : RoomDatabase() {

    abstract fun cachedValidationDao(): CachedValidationDao
    abstract fun callEventDao(): CallEventDao
    abstract fun pendingReportDao(): PendingReportDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS call_events (
                        id TEXT PRIMARY KEY NOT NULL,
                        number_hash TEXT NOT NULL,
                        number_masked TEXT NOT NULL,
                        verdict TEXT NOT NULL,
                        spam_score INTEGER,
                        reason TEXT,
                        occurred_at_millis INTEGER NOT NULL,
                        action_taken TEXT NOT NULL,
                        synced_feedback INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_reports (
                        id TEXT PRIMARY KEY NOT NULL,
                        number_hash TEXT NOT NULL,
                        number_masked TEXT NOT NULL,
                        report_type TEXT NOT NULL,
                        description TEXT,
                        is_feedback INTEGER NOT NULL DEFAULT 0,
                        feedback_type TEXT,
                        call_event_id TEXT NOT NULL,
                        created_at_millis INTEGER NOT NULL,
                        retry_count INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
    }
}
