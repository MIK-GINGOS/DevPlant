package com.mixesgates.devplant.mixes.mm_history;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Diagnosis;
import com.mixesgates.devplant.contracts.Kind;
import com.mixesgates.devplant.contracts.Scan;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.util.ArrayList;
import java.util.List;

/** Past scans, grouped by date. */
public class HistoryFragment extends Fragment {

    private static final int LIMIT = 500;

    private RecyclerView recycler;
    private View empty;
    private final Adapter adapter = new Adapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        FrameLayout root = new FrameLayout(c);
        recycler = new RecyclerView(c);
        recycler.setLayoutManager(new LinearLayoutManager(c));
        recycler.setClipToPadding(false);
        recycler.setPadding(Ui.dp(c, 16), Ui.dp(c, 4), Ui.dp(c, 16), Ui.dp(c, Ui.ISLAND_CLEARANCE_DP));
        recycler.setAdapter(adapter);
        root.addView(recycler);
        empty = Ui.empty(c, "No scans yet", "Scanned leaves appear here by date.");
        root.addView(empty);
        empty.setVisibility(View.GONE);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Store.of(requireContext()).listen(getViewLifecycleOwner(), this::reload, Changes.Topic.SCANS);
        reload();
    }

    private void reload() {
        Store.of(requireContext()).load(getViewLifecycleOwner(), s -> s.scans().recent(LIMIT), scans -> {
            List<Object> items = new ArrayList<>();
            String lastDay = null;
            for (Scan scan : scans) {
                String day = Ui.day(scan.takenAt);
                if (!day.equals(lastDay)) {
                    items.add(day);
                    lastDay = day;
                }
                items.add(scan);
            }
            adapter.set(items);
            empty.setVisibility(scans.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int HEADER = 0;
        private static final int SCAN = 1;
        private List<Object> items = new ArrayList<>();

        void set(List<Object> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof String ? HEADER : SCAN;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context c = parent.getContext();
            if (viewType == HEADER) {
                TextView t = Ui.text(c, "", Ui.Type.LABEL);
                t.setPadding(Ui.dp(c, 4), Ui.dp(c, 16), 0, Ui.dp(c, 4));
                return new RecyclerView.ViewHolder(t) { };
            }
            return new ScanHolder(c);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = items.get(position);
            if (holder instanceof ScanHolder) ((ScanHolder) holder).bind((Scan) item);
            else ((TextView) holder.itemView).setText((String) item);
        }
    }

    private final class ScanHolder extends RecyclerView.ViewHolder {
        private final ImageView thumb;
        private final TextView title;
        private final TextView caption;
        private final LinearLayout pills;

        ScanHolder(Context c) {
            super(Ui.card(c));
            LinearLayout card = (LinearLayout) itemView;
            card.setPadding(Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Ui.dp(c, 8);
            card.setLayoutParams(lp);
            LinearLayout row = Ui.row(c);
            thumb = new ImageView(c);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackground(Ui.rounded(Ui.color(c, R.color.mm_line), Ui.dp(c, 12)));
            thumb.setClipToOutline(true);
            row.addView(thumb, new LinearLayout.LayoutParams(Ui.dp(c, 60), Ui.dp(c, 60)));
            LinearLayout texts = Ui.column(c);
            texts.setPadding(Ui.dp(c, 12), 0, 0, 0);
            title = Ui.text(c, "", Ui.Type.HEADING);
            caption = Ui.text(c, "", Ui.Type.CAPTION);
            pills = Ui.row(c);
            texts.addView(title);
            texts.addView(caption);
            texts.addView(Ui.gap(pills, 4));
            row.addView(texts, Ui.weight(1));
            card.addView(row);
        }

        void bind(Scan scan) {
            Context c = itemView.getContext();
            Diagnosis d = scan.diagnosis;
            title.setText(d == null ? "Not diagnosed" : d.name);
            caption.setText((scan.fieldName == null ? "No field" : scan.fieldName) + " · " + Ui.time(scan.takenAt));
            pills.removeAllViews();
            if (d != null) {
                pills.addView(Ui.pill(c, Ui.kindLabel(d.kind), Ui.kindColor(d.kind)));
                if (d.kind == Kind.DISEASE || d.kind == Kind.PEST) {
                    pills.addView(Ui.pill(c, Ui.severityLabel(d.severity), Ui.severityColor(d.severity)));
                }
            }
            Store.of(c).photos.load(thumb, scan.photoPath, Ui.dp(c, 60));
            Ui.tappable(itemView, v -> Nav.of(HistoryFragment.this).openResult(scan.id));
        }
    }
}
