package com.mixesgates.devplant.gates.gm_reports;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Reports live in their own file, so devplant.db and its schema stay as they are. */
@Database(entities = {ReportRow.class, QuestionRow.class}, version = 2, exportSchema = false)
public abstract class ReportsDatabase extends RoomDatabase {

    public abstract ReportDao reports();

    public abstract QuestionDao questions();

    /** Version 2: the officer's note on a report, and questions to officers. */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `reports` ADD COLUMN `officerNote` TEXT");
            db.execSQL("CREATE TABLE IF NOT EXISTS `questions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`reportId` INTEGER NOT NULL, `askedAt` INTEGER NOT NULL, `text` TEXT NOT NULL, "
                    + "`cropName` TEXT, `answer` TEXT, `answeredBy` TEXT, `answeredAt` INTEGER NOT NULL, "
                    + "`sync` TEXT NOT NULL)");
        }
    };

    public static ReportsDatabase open(Context context, String fileName) {
        return Room.databaseBuilder(context.getApplicationContext(), ReportsDatabase.class, fileName)
                .addMigrations(MIGRATION_1_2)
                .build();
    }
}
