package ar.com.patova.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ar.com.patova.data.local.daos.BlacklistDao
import ar.com.patova.data.local.daos.LocalPreferencesDao
import ar.com.patova.data.local.daos.WhitelistDao
import ar.com.patova.data.local.entities.BlacklistEntity
import ar.com.patova.data.local.entities.LocalPreferencesEntity
import ar.com.patova.data.local.entities.WhitelistEntity

@Database(
    entities = [
        CachedValidationEntity::class,
        CallEventEntity::class,
        PendingReportEntity::class,
        LocalPreferencesEntity::class,
        WhitelistEntity::class,
        BlacklistEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class PatovaDatabase : RoomDatabase() {

    abstract fun cachedValidationDao(): CachedValidationDao
    abstract fun callEventDao(): CallEventDao
    abstract fun pendingReportDao(): PendingReportDao
    abstract fun localPreferencesDao(): LocalPreferencesDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun blacklistDao(): BlacklistDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_preferences (
                        id INTEGER PRIMARY KEY NOT NULL,
                        strict_mode INTEGER NOT NULL DEFAULT 0,
                        block_unknown INTEGER NOT NULL DEFAULT 0,
                        spam_threshold REAL NOT NULL DEFAULT 0.75,
                        sync_enabled INTEGER NOT NULL DEFAULT 1,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS whitelist (
                        phone_hash TEXT PRIMARY KEY NOT NULL,
                        label TEXT NOT NULL,
                        added_at INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS blacklist (
                        phone_hash TEXT PRIMARY KEY NOT NULL,
                        reason TEXT NOT NULL,
                        added_at INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
