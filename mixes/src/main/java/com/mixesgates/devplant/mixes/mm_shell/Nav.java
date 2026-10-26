package com.mixesgates.devplant.mixes.mm_shell;

import androidx.fragment.app.Fragment;

/** How screens move between each other. Implemented by {@link ShellActivity}. */
public interface Nav {

    int TAB_SCAN = 0;
    int TAB_HISTORY = 1;
    int TAB_FIELDS = 2;
    int TAB_LIBRARY = 3;
    int TAB_DASHBOARD = 4;
    int TAB_CATALOG = 5;

    void showTab(int tab);

    void openResult(long scanId);

    void openGuide(String key);

    /** One plant from the plant catalogue. */
    void openPlant(String key);

    void openAlerts();

    void openSearch();

    void openSettings();

    void back();

    static Nav of(Fragment fragment) {
        return (Nav) fragment.requireActivity();
    }
}
