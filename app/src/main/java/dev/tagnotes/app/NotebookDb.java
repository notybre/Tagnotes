package dev.tagnotes.app;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {NoteRow.class}, version = 1, exportSchema = false)
public abstract class NotebookDb extends RoomDatabase {
    public abstract NoteDao notes();
}
