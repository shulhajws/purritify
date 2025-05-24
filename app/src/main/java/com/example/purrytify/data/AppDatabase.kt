package com.example.purrytify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.dao.AnalyticsDao
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.entity.ListeningSessionEntity

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE songs ADD COLUMN updatedAt INTEGER")
        db.execSQL("ALTER TABLE songs ADD COLUMN rank INTEGER")
        db.execSQL("ALTER TABLE songs ADD COLUMN country TEXT")
        db.execSQL("ALTER TABLE songs ADD COLUMN isFromServer INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `listening_sessions`")

        // Create Listening Sessions Table For Analytics
        db.execSQL("""
            CREATE TABLE `listening_sessions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `userId` INTEGER NOT NULL,
                `songId` INTEGER NOT NULL,
                `startTime` INTEGER NOT NULL,
                `endTime` INTEGER,
                `actualListenDurationMs` INTEGER NOT NULL,
                `wasCompleted` INTEGER NOT NULL,
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `dayOfMonth` INTEGER NOT NULL,
                `dateString` TEXT NOT NULL
            )
        """)

        // Create Indices For Better Query Performance
        db.execSQL("""
            CREATE INDEX `index_listening_sessions_userId` 
            ON `listening_sessions` (`userId`)
        """)

        db.execSQL("""
            CREATE INDEX `index_listening_sessions_songId` 
            ON `listening_sessions` (`songId`)
        """)

        db.execSQL("""
            CREATE INDEX `index_listening_sessions_year_month` 
            ON `listening_sessions` (`userId`, `year`, `month`)
        """)

        db.execSQL("""
            CREATE INDEX `index_listening_sessions_dateString` 
            ON `listening_sessions` (`userId`, `dateString`)
        """)

        db.execSQL("""
            CREATE INDEX `index_listening_sessions_active` 
            ON `listening_sessions` (`userId`, `endTime`)
        """)
    }
}

@Database(
    entities = [SongEntity::class, ListeningSessionEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "purrytify_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}