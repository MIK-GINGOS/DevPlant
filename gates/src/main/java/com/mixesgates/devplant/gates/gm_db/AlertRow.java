package com.mixesgates.devplant.gates.gm_db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "alerts",
        foreignKeys = @ForeignKey(entity = FieldRow.class, parentColumns = "id",
                childColumns = "fieldId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("fieldId"), @Index("createdAt")})
public class AlertRow {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long createdAt;
    /** WATCH or WARNING. */
    @NonNull
    public String level = "WATCH";
    @NonNull
    public String title = "";
    public String body;
    public String subjectKey;
    public Long fieldId;
    public boolean read;
}
