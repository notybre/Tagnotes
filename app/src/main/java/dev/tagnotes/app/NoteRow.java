package dev.tagnotes.app;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteRow {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String title = "";
    @NonNull public String body = "";
    @NonNull public String tags = "";
    public long modified;
}
