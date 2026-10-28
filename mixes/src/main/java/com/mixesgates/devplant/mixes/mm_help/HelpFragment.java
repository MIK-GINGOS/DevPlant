package com.mixesgates.devplant.mixes.mm_help;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Question;
import com.mixesgates.devplant.contracts.Report;
import com.mixesgates.devplant.contracts.ReportStore;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_reports.Reports;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.List;

/**
 * Ask an extension officer (CropGuard): a question, maybe about one report, sent as soon as the
 * phone has signal, and the answers that came back.
 */
public class HelpFragment extends Fragment {

    private static final String ARG_REPORT = "reportId";

    /** @param reportId the report the question is about, or 0 */
    public static HelpFragment newInstance(long reportId) {
        HelpFragment f = new HelpFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_REPORT, reportId);
        f.setArguments(args);
        return f;
    }

    private static final class Data {
        Report report;
        List<Question> questions;
    }

    private LinearLayout body;
    private LinearLayout history;
    private EditText question;
    private TextView error;
    private long reportId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        reportId = getArguments() == null ? 0 : getArguments().getLong(ARG_REPORT);
        LinearLayout root = Ui.screen(c);
        root.addView(Ui.topBar(c, "Ask an officer", () -> Nav.of(this).back()));
        Ui.Page page = Ui.page(c, true); // the menu island stays on top of this screen
        body = page.body;
        root.addView(page.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        body.addView(Ui.text(c, "Ask an extension officer", Ui.Type.TITLE));
        body.addView(Ui.gap(Ui.text(c, "Describe what you see: which plants, how many, since when. An officer "
                + "answers here, usually within a few days.", Ui.Type.BODY), 8));

        LinearLayout ask = Ui.card(c);
        question = new EditText(c);
        question.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        question.setMinLines(3);
        question.setGravity(Gravity.TOP | Gravity.START);
        question.setHint("For example: Yellow streaks on young maize leaves in half the field since last week.");
        ask.addView(question);
        error = Ui.text(c, "", Ui.Type.CAPTION);
        error.setTextColor(Ui.color(c, R.color.mm_severe));
        error.setVisibility(View.GONE);
        ask.addView(Ui.gap(error, 4));
        View send = Ui.gap(Ui.button(c, "Send to an officer", true), 10);
        send.setOnClickListener(v -> send());
        ask.addView(send);
        ask.addView(Ui.gap(Ui.text(c, reportId == 0
                ? "Sent with your country and district. No photos."
                : "Sent with this report's details: crop, result, how sure, date and area. No photos.",
                Ui.Type.CAPTION), 8));
        body.addView(ask);

        history = Ui.column(c);
        body.addView(history);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Store.of(requireContext()).listen(getViewLifecycleOwner(), this::reload, Changes.Topic.SCANS);
        reload();
        // Fetch new answers; when they arrive the SCANS change reloads the screen.
        Reports.of(requireContext()).syncSoon();
    }

    private void reload() {
        Context app = requireContext().getApplicationContext();
        Store.of(app).load(getViewLifecycleOwner(), s -> {
            Data d = new Data();
            ReportStore reports = Reports.of(app);
            d.questions = reports.questions();
            if (reportId != 0) {
                for (Report r : reports.recent(500)) if (r.id == reportId) d.report = r;
            }
            return d;
        }, this::render);
    }

    private void render(Data data) {
        Context c = requireContext();
        history.removeAllViews();
        if (data.report != null) {
            LinearLayout about = Ui.card(c);
            about.setBackground(Ui.rounded(Ui.color(c, R.color.mm_leaf_soft), Ui.dp(c, 18)));
            about.addView(Ui.text(c, "About this report", Ui.Type.LABEL));
            String title = data.report.isUncertain() ? "Not recognised yet"
                    : data.report.resultName == null ? "Not diagnosed" : data.report.resultName;
            about.addView(Ui.gap(Ui.text(c, title, Ui.Type.HEADING), 4));
            about.addView(Ui.text(c, (data.report.cropName == null ? "" : data.report.cropName + " · ")
                    + Ui.dateTime(data.report.createdAt), Ui.Type.CAPTION));
            history.addView(about);
        }
        if (data.questions.isEmpty()) return;
        history.addView(Ui.gap(Ui.text(c, "Your questions", Ui.Type.HEADING), 24));
        for (Question q : data.questions) history.addView(card(c, q));
    }

    private View card(Context c, Question q) {
        LinearLayout card = Ui.card(c);
        card.addView(Ui.text(c, q.text, Ui.Type.BODY));
        String meta = (q.cropName == null ? "" : q.cropName + " · ") + Ui.dateTime(q.askedAt);
        card.addView(Ui.gap(Ui.text(c, meta, Ui.Type.CAPTION), 4));
        if (q.isAnswered()) {
            LinearLayout answer = Ui.column(c);
            answer.setBackground(Ui.rounded(Ui.color(c, R.color.mm_leaf_soft), Ui.dp(c, 12)));
            answer.setPadding(Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
            answer.addView(Ui.text(c, q.answer, Ui.Type.BODY));
            String by = (q.answeredBy == null ? "Extension officer" : q.answeredBy) + " · " + Ui.dateTime(q.answeredAt);
            answer.addView(Ui.gap(Ui.text(c, by, Ui.Type.CAPTION), 4));
            card.addView(Ui.gap(answer, 10));
        } else {
            String state = q.sync == Report.Sync.SENT ? "Sent. Waiting for an answer" : "Waiting for signal to send";
            LinearLayout row = Ui.row(c);
            row.addView(Ui.pill(c, state, q.sync == Report.Sync.SENT ? R.color.mm_leaf : R.color.mm_unsure));
            card.addView(Ui.gap(row, 8));
        }
        return card;
    }

    private void send() {
        String text = question.getText().toString().trim();
        if (text.length() < 3) {
            error.setText("Write your question first.");
            error.setVisibility(View.VISIBLE);
            return;
        }
        error.setVisibility(View.GONE);
        question.setText("");
        Context app = requireContext().getApplicationContext();
        long about = reportId;
        Store.of(app).write(s -> Reports.of(app).ask(text, about));
    }
}
