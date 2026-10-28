package com.mixesgates.devplant.mixes.mm_shell;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_alerts.AlertsFragment;
import com.mixesgates.devplant.mixes.mm_catalog.CatalogFragment;
import com.mixesgates.devplant.mixes.mm_catalog.PlantFragment;
import com.mixesgates.devplant.mixes.mm_dashboard.DashboardFragment;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_fields.FieldsFragment;
import com.mixesgates.devplant.mixes.mm_history.HistoryFragment;
import com.mixesgates.devplant.mixes.mm_library.GuideEntryFragment;
import com.mixesgates.devplant.mixes.mm_library.LibraryFragment;
import com.mixesgates.devplant.mixes.mm_reports.Reports;
import com.mixesgates.devplant.mixes.mm_reports.ReportsFragment;
import com.mixesgates.devplant.mixes.mm_result.ResultFragment;
import com.mixesgates.devplant.mixes.mm_scan.ScanFragment;
import com.mixesgates.devplant.mixes.mm_search.SearchFragment;
import com.mixesgates.devplant.mixes.mm_settings.SettingsFragment;
import com.mixesgates.devplant.mixes.mm_settings.ThemePrefs;
import com.mixesgates.devplant.mixes.mm_theme.Ui;
import com.mixesgates.devplant.mixes.mm_welcome.WelcomeFragment;

/** The one activity: header on top, pager of main screens, the menu at the bottom, details above. */
public class ShellActivity extends AppCompatActivity implements Nav {

    private static final String[] SUBTITLES = {"Scan a leaf", "Past scans", "My crops and fields",
            "Pest & disease guide", "Field health", "Plant catalogue"};

    private Header header;
    private ControlMenu menu;
    private ViewPager2 pager;
    private Store store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePrefs.apply(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        store = Store.of(this);

        FrameLayout root = new FrameLayout(this);
        LinearLayout column = Ui.column(this);
        header = new Header(this, this);
        column.addView(header);
        pager = new ViewPager2(this);
        pager.setId(R.id.mm_pager);
        pager.setOffscreenPageLimit(1);
        pager.setAdapter(new Pages(this));
        column.addView(pager, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(column);

        // Tiles without an action are not built yet; they say so when tapped.
        menu = new ControlMenu(this, new ControlMenu.Tile[]{
                ControlMenu.Tile.page("Home", R.drawable.mm_ic_cc_home, TAB_DASHBOARD, () -> showTab(TAB_DASHBOARD)),
                ControlMenu.Tile.planned("Security", R.drawable.mm_ic_cc_shield),
                ControlMenu.Tile.page("Fields", R.drawable.mm_ic_cc_space, TAB_FIELDS, () -> showTab(TAB_FIELDS)),
        }, new ControlMenu.Tile[]{
                ControlMenu.Tile.action("Alerts", R.drawable.mm_ic_cc_bell_line, this::openAlerts),
                ControlMenu.Tile.planned("Location", R.drawable.mm_ic_cc_location),
                ControlMenu.Tile.action("Reports", R.drawable.mm_ic_cc_share, () -> open(new ReportsFragment())),
                ControlMenu.Tile.action("Settings", R.drawable.mm_ic_cc_sliders, this::openSettings),
                ControlMenu.Tile.page("Camera", R.drawable.mm_ic_cc_camera, TAB_SCAN, () -> showTab(TAB_SCAN)),
                ControlMenu.Tile.planned("Add", R.drawable.mm_ic_cc_add),
                ControlMenu.Tile.page("Catalog", R.drawable.mm_ic_cc_catalog, TAB_CATALOG, () -> showTab(TAB_CATALOG)),
                ControlMenu.Tile.planned("Active Profile", R.drawable.mm_ic_cc_profile),
        });
        FrameLayout.LayoutParams menuLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        menuLp.leftMargin = Ui.dp(this, 16);
        menuLp.rightMargin = Ui.dp(this, 16);
        root.addView(menu, menuLp);
        menu.setBackdropSource(column);

        // Back closes the open menu before anything else.
        OnBackPressedCallback closeMenu = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                menu.collapse();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeMenu);
        menu.setExpandListener(closeMenu::setEnabled);

        FrameLayout overlay = new FrameLayout(this);
        overlay.setId(R.id.mm_overlay);
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // Opaque only while a detail screen is open, so the system-bar gaps are not see-through.
        FragmentManager fm = getSupportFragmentManager();
        Runnable overlayBackground = () -> overlay.setBackgroundColor(
                fm.getBackStackEntryCount() > 0 ? Ui.color(this, R.color.mm_bg) : 0);
        fm.addOnBackStackChangedListener(overlayBackground::run);
        overlayBackground.run();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            header.setPadding(header.getPaddingLeft(), bars.top + Ui.dp(this, 8),
                    header.getPaddingRight(), header.getPaddingBottom());
            menuLp.bottomMargin = bars.bottom + Ui.dp(this, 16);
            menu.setLayoutParams(menuLp);
            overlay.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                menu.select(position);
                menu.frostSoon();
                header.setSubtitle(SUBTITLES[position]);
            }
        });
        if (savedInstanceState == null) pager.setCurrentItem(TAB_DASHBOARD, false);
        menu.select(pager.getCurrentItem());
        header.setSubtitle(SUBTITLES[pager.getCurrentItem()]);

        store.listen(this, this::refreshUnread, Changes.Topic.ALERTS);
        refreshUnread();

        // First launch: the welcome screen asks about sharing before anything else.
        if (savedInstanceState == null) {
            store.load(this, s -> Reports.of(this).consent(), consent -> {
                if (!consent.decided) open(new WelcomeFragment());
            });
        }
    }

    private void refreshUnread() {
        store.load(this, s -> s.alerts().unreadCount(), header::setUnread);
    }

    private void open(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.mm_overlay, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showTab(int tab) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        pager.setCurrentItem(tab, false);
    }

    @Override
    public void openResult(long scanId) {
        open(ResultFragment.newInstance(scanId));
    }

    @Override
    public void openGuide(String key) {
        open(GuideEntryFragment.newInstance(key));
    }

    @Override
    public void openPlant(String key) {
        open(PlantFragment.newInstance(key));
    }

    @Override
    public void openAlerts() {
        open(new AlertsFragment());
    }

    @Override
    public void openSearch() {
        open(new SearchFragment());
    }

    @Override
    public void openSettings() {
        open(new SettingsFragment());
    }

    @Override
    public void back() {
        getOnBackPressedDispatcher().onBackPressed();
    }

    private static final class Pages extends FragmentStateAdapter {
        Pages(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case TAB_HISTORY: return new HistoryFragment();
                case TAB_FIELDS: return new FieldsFragment();
                case TAB_LIBRARY: return new LibraryFragment();
                case TAB_DASHBOARD: return new DashboardFragment();
                case TAB_CATALOG: return new CatalogFragment();
                default: return new ScanFragment();
            }
        }

        @Override
        public int getItemCount() {
            return SUBTITLES.length;
        }
    }
}
