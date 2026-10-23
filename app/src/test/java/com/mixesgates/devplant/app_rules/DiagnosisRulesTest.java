package com.mixesgates.devplant.app_rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.mixesgates.devplant.contracts.Alert;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.contracts.Severity;
import com.mixesgates.devplant.gates.gm_model.Label;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiagnosisRulesTest {

    private final DiagnosisRules rules = new DiagnosisRules();

    private final DiagnosisRules.Lookup lookup = new DiagnosisRules.Lookup() {
        @Override
        public Kind kindOf(String key) {
            switch (key) {
                case "late_blight": return Kind.DISEASE;
                case "fall_armyworm": return Kind.PEST;
                default: return null;
            }
        }

        @Override
        public String nameOf(String key) {
            switch (key) {
                case "late_blight": return "Late blight";
                case "fall_armyworm": return "Fall armyworm";
                default: return null;
            }
        }
    };

    private static List<Label> labels(Object... nameThenConfidence) {
        Label[] out = new Label[nameThenConfidence.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Label((String) nameThenConfidence[2 * i], (Float) nameThenConfidence[2 * i + 1]);
        }
        return Arrays.asList(out);
    }

    @Test
    public void noLeafAsksForRetake() {
        Verdict v = rules.decide(labels("late_blight", 0.9f), new LeafStats(0.05f, 0f), lookup);
        assertTrue(v.retake);
    }

    @Test
    public void lowConfidenceAsksForRetake() {
        Verdict v = rules.decide(labels("late_blight", 0.3f), new LeafStats(0.6f, 0.2f), lookup);
        assertTrue(v.retake);
        assertTrue(rules.decide(Collections.emptyList(), new LeafStats(0.6f, 0f), lookup).retake);
    }

    @Test
    public void healthyLeafHasNoSeverity() {
        Verdict v = rules.decide(labels("healthy", 0.9f), new LeafStats(0.7f, 0.02f), lookup);
        assertFalse(v.retake);
        assertEquals(Kind.HEALTHY, v.kind);
        assertEquals(Severity.NONE, v.severity);
        assertFalse(v.isProblem());
    }

    @Test
    public void diseaseSeverityFollowsDamage() {
        Verdict v = rules.decide(labels("late_blight", 0.85f, "healthy", 0.1f),
                new LeafStats(0.7f, 0.4f), lookup);
        assertEquals(Kind.DISEASE, v.kind);
        assertEquals("Late blight", v.name);
        assertEquals(Severity.SEVERE, v.severity);
        assertEquals("Healthy 10%", v.alternatives);
        assertEquals(Severity.MILD, DiagnosisRules.severityFor(0.05f));
        assertEquals(Severity.MODERATE, DiagnosisRules.severityFor(0.2f));
    }

    @Test
    public void tentativeMatchSaysSo() {
        Verdict v = rules.decide(labels("fall_armyworm", 0.5f), new LeafStats(0.7f, 0.15f), lookup);
        assertEquals(Kind.PEST, v.kind);
        assertTrue(v.advice.startsWith("Possible match"));
    }

    @Test
    public void unknownLabelIsUnsure() {
        Verdict v = rules.decide(labels("mystery_mould", 0.8f), new LeafStats(0.7f, 0.2f), lookup);
        assertEquals(Kind.UNSURE, v.kind);
        assertEquals("Mystery mould", v.name);
        assertNull(rules.warningFor(v, 5, "North plot"));
    }

    @Test
    public void repeatedProblemRaisesWarning() {
        Verdict v = rules.decide(labels("late_blight", 0.9f), new LeafStats(0.7f, 0.05f), lookup);
        assertNull(rules.warningFor(v, 1, "North plot"));
        DiagnosisRules.Warning w = rules.warningFor(v, 3, "North plot");
        assertNotNull(w);
        assertEquals(Alert.Level.WARNING, w.level);
        assertTrue(w.title.contains("North plot"));
    }

    @Test
    public void severeProblemRaisesWatch() {
        Verdict v = rules.decide(labels("late_blight", 0.9f), new LeafStats(0.7f, 0.5f), lookup);
        DiagnosisRules.Warning w = rules.warningFor(v, 1, null);
        assertNotNull(w);
        assertEquals(Alert.Level.WATCH, w.level);
    }
}
