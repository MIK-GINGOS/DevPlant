package com.mixesgates.devplant.mixes.mm_library;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.mixesgates.devplant.contracts.GuideEntry;
import com.mixesgates.devplant.contracts.Treatment;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.List;

/** Guide rows and treatment cards, shared by the guide, result and search screens. */
public final class GuideViews {

    private GuideViews() {
    }

    public static View entryRow(Context c, GuideEntry e, View.OnClickListener click) {
        LinearLayout card = Ui.card(c);
        LinearLayout top = Ui.row(c);
        top.addView(Ui.text(c, e.name, Ui.Type.HEADING), Ui.weight(1));
        top.addView(Ui.pill(c, Ui.kindLabel(e.kind), Ui.kindColor(e.kind)));
        card.addView(top);
        android.widget.TextView sci = Ui.text(c, e.scientificName, Ui.Type.CAPTION);
        sci.setTypeface(sci.getTypeface(), android.graphics.Typeface.ITALIC);
        card.addView(Ui.gap(sci, 2));
        card.addView(Ui.gap(Ui.text(c, e.crops, Ui.Type.CAPTION), 2));
        return Ui.tappable(card, click);
    }

    public static void treatments(Context c, LinearLayout into, List<Treatment> treatments) {
        for (Treatment t : treatments) {
            LinearLayout card = Ui.card(c);
            LinearLayout top = Ui.row(c);
            top.addView(Ui.text(c, t.title, Ui.Type.HEADING), Ui.weight(1));
            top.addView(Ui.pill(c, methodLabel(t.method), methodColor(t.method)));
            card.addView(top);
            card.addView(Ui.gap(Ui.text(c, t.detail, Ui.Type.BODY), 6));
            into.addView(card);
        }
        into.addView(Ui.gap(Ui.text(c,
                "Always follow the product label and local extension advice for chemical use.",
                Ui.Type.CAPTION), 4));
    }

    static String methodLabel(Treatment.Method m) {
        switch (m) {
            case CHEMICAL: return "Chemical";
            case ORGANIC: return "Organic";
            default: return "Cultural";
        }
    }

    static int methodColor(Treatment.Method m) {
        switch (m) {
            case CHEMICAL: return R.color.mm_disease;
            case ORGANIC: return R.color.mm_pest;
            default: return R.color.mm_healthy;
        }
    }
}
