package com.mixesgates.devplant.mixes.mm_reports;

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.mixesgates.devplant.contracts.Report;
import com.mixesgates.devplant.contracts.ReportServices;
import com.mixesgates.devplant.contracts.ReportStore;
import com.mixesgates.devplant.mixes.R;

/** The screens' way to the reports, and the words and colours for them. */
public final class Reports {

    private Reports() {
    }

    /** Blocks like every contract call: use it inside Store.load or Store.write. */
    public static ReportStore of(Context context) {
        Context app = context.getApplicationContext();
        if (!(app instanceof ReportServices.Provider)) {
            throw new IllegalStateException("The Application must implement ReportServices.Provider");
        }
        return ((ReportServices.Provider) app).reportServices().reports();
    }

    /** Opens a detail screen over the pager, the same way the shell does. */
    public static void open(Fragment from, Fragment screen) {
        from.requireActivity().getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.mm_overlay, screen)
                .addToBackStack(null)
                .commit();
    }

    public static String sourceLabel(Report.Source source) {
        switch (source) {
            case AI_MODEL: return "AI result";
            case EXPERT: return "Expert checked";
            default: return "Simulated";
        }
    }

    /** One sentence for the result screen on where the result came from. */
    public static String sourceSentence(Report.Source source) {
        switch (source) {
            case AI_MODEL: return "This result comes from the AI model on this phone.";
            case EXPERT: return "An expert has checked this result.";
            default: return "This result comes from a simple colour check, not a trained AI model.";
        }
    }

    public static String verificationLabel(Report.Verification v) {
        switch (v) {
            case UNDER_REVIEW: return "Under review";
            case FIELD_VERIFIED: return "Field verified";
            default: return "Reported";
        }
    }

    public static int verificationColor(Report.Verification v) {
        switch (v) {
            case UNDER_REVIEW: return R.color.mm_moderate;
            case FIELD_VERIFIED: return R.color.mm_healthy;
            default: return R.color.mm_unsure;
        }
    }

    public static String syncLabel(Report.Sync sync) {
        switch (sync) {
            case QUEUED: return "Waiting to send";
            case SENT: return "Sent";
            default: return "On this phone";
        }
    }

    public static String area(String country, String district) {
        if (country == null && district == null) return null;
        if (district == null || district.isEmpty()) return country;
        if (country == null) return district;
        return district + ", " + country;
    }
}
