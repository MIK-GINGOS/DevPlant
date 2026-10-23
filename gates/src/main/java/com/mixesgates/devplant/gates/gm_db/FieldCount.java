package com.mixesgates.devplant.gates.gm_db;

/** Scan counts grouped by field; fieldId is null for scans without a field. */
public class FieldCount {
    public Long fieldId;
    public String fieldName;
    public int scans;
    public int healthy;
    public int diseases;
    public int pests;
    public int unsure;
}
