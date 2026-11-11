package dev.tagnotes.app;

import androidx.room.*;
import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY modified DESC, id DESC") List<NoteRow> list();
    @Insert long insert(NoteRow note);
    @Update int update(NoteRow note);
    @Query("DELETE FROM notes WHERE id = :id") int delete(long id);
}
