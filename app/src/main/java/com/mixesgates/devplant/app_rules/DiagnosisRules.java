package com.mixesgates.devplant.app_rules;

import com.mixesgates.devplant.contracts.Alert;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.contracts.Severity;
import com.mixesgates.devplant.gates.gm_model.Label;

import java.util.List;
import java.util.Locale;

/** Picks pest or disease, severity and treatment advice from the model's top labels. */
public final class DiagnosisRules {

    /** Tells the rules what a model label means. */
    public interface Lookup {
        /** HEALTHY, DISEASE or PEST, or null for a label the guide does not know. */
        Kind kindOf(String key);

        /** Display name, or null. */
        String nameOf(String key);
    }

    /** A warning to raise for a field. */
    public static final class Warning {
        public final Alert.Level level;
        public final String title;
        public final String body;

        Warning(Alert.Level level, String title, String body) {
            this.level = level;
            this.title = title;
            this.body = body;
        }
    }

    public static final String HEALTHY = "healthy";
    /** Below this the model is guessing: ask for another photo. */
    public static final float RETAKE_BELOW = 0.40f;
    /** Below this the answer is shown as a possible match. */
    public static final float TENTATIVE_BELOW = 0.60f;
    /** Less leaf than this in the picture means the photo missed the leaf. */
    public static final float MIN_LEAF = 0.15f;
    /** Same problem this many times in one field within the window raises a warning. */
    public static final int OUTBREAK_COUNT = 3;
    public static final long OUTBREAK_WINDOW_MS = 14L * 24 * 60 * 60 * 1000;

    public Verdict decide(List<Label> labels, LeafStats stats, Lookup lookup) {
        if (stats.leafFraction < MIN_LEAF) {
            return Verdict.retake("No leaf found. Fill the frame with one leaf, in daylight.");
        }
        if (labels.isEmpty() || labels.get(0).confidence < RETAKE_BELOW) {
            return Verdict.retake("Not sure what this is. Move closer, hold steady and avoid shadows.");
        }

        Label top = labels.get(0);
        Kind kind = HEALTHY.equals(top.name) ? Kind.HEALTHY : lookup.kindOf(top.name);
        if (kind == null) kind = Kind.UNSURE;
        String name = kind == Kind.HEALTHY ? "Healthy" : lookup.nameOf(top.name);
        if (name == null) name = pretty(top.name);

        Severity severity = kind == Kind.HEALTHY ? Severity.NONE : severityFor(stats.damage);
        String advice = adviceFor(kind, severity, top.confidence < TENTATIVE_BELOW);
        return new Verdict(false, null, kind, top.name, name, top.confidence, severity,
                stats.damage, alternatives(labels, lookup), advice);
    }

    static Severity severityFor(float damage) {
        if (damage < 0.10f) return Severity.MILD;
        if (damage < 0.30f) return Severity.MODERATE;
        return Severity.SEVERE;
    }

    /**
     * @param recentInField scans of this problem in this field within the outbreak window,
     *                      including this one
     * @return null when no warning is needed
     */
    public Warning warningFor(Verdict verdict, int recentInField, String fieldName) {
        if (!verdict.isProblem()) return null;
        String where = fieldName == null ? "your crops" : fieldName;
        if (recentInField >= OUTBREAK_COUNT) {
            return new Warning(Alert.Level.WARNING,
                    verdict.name + " spreading in " + where,
                    recentInField + " scans in the last 14 days found " + verdict.name.toLowerCase(Locale.ROOT)
                            + ". Check the whole field and start treatment now.");
        }
        if (verdict.severity == Severity.SEVERE) {
            return new Warning(Alert.Level.WATCH,
                    "Severe " + verdict.name.toLowerCase(Locale.ROOT) + " in " + where,
                    "A scan found heavy damage. Scan nearby plants to see how far it has spread.");
        }
        return null;
    }

    static String adviceFor(Kind kind, Severity severity, boolean tentative) {
        switch (kind) {
            case HEALTHY:
                return "No sign of pest or disease. Keep scouting weekly.";
            case UNSURE:
                return "This does not match anything in the guide. Compare with the guide or ask an extension officer.";
            default:
                String prefix = tentative ? "Possible match: take another photo to confirm. " : "";
                switch (severity) {
                    case SEVERE:
                        return prefix + "Serious damage. Treat now and check the rest of the field today.";
                    case MODERATE:
                        return prefix + "Spreading. Start treatment this week and scan nearby plants.";
                    default:
                        return prefix + "Early stage. Remove affected leaves and recheck in 3 to 5 days.";
                }
        }
    }

    private static String alternatives(List<Label> labels, Lookup lookup) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < labels.size() && i < 3; i++) {
            Label l = labels.get(i);
            if (l.confidence < 0.05f) break;
            String name = HEALTHY.equals(l.name) ? "Healthy" : lookup.nameOf(l.name);
            if (sb.length() > 0) sb.append(", ");
            sb.append(name == null ? pretty(l.name) : name)
                    .append(' ').append(Math.round(l.confidence * 100)).append('%');
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    static String pretty(String label) {
        String s = label.replace('_', ' ').trim();
        return s.isEmpty() ? "Unknown" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
