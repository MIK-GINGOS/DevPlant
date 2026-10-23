package com.mixesgates.devplant.gates.gm_db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Advice for one disease or pest, keyed by its subjectKey. */
@Entity(tableName = "treatments", indices = @Index("subjectKey"))
public class TreatmentRow {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String subjectKey = "";
    /** CULTURAL, ORGANIC or CHEMICAL. */
    @NonNull
    public String method = "CULTURAL";
    @NonNull
    public String title = "";
    public String detail;
    public int sortOrder;
}
