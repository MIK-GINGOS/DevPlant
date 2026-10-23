package com.mixesgates.devplant.mixes.mm_scan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.util.concurrent.ListenableFuture;
import com.mixesgates.devplant.contracts.Changes;
import com.mixesgates.devplant.contracts.Field;
import com.mixesgates.devplant.contracts.ScanOutcome;
import com.mixesgates.devplant.mixes.R;
import com.mixesgates.devplant.mixes.mm_data.Store;
import com.mixesgates.devplant.mixes.mm_shell.Nav;
import com.mixesgates.devplant.mixes.mm_theme.Ui;

import java.io.File;
import java.util.List;

/** Camera that frames one leaf. The photo stays on the phone. */
public class ScanFragment extends Fragment {

    private Store store;
    private PreviewView preview;
    private ImageCapture capture;
    private ProcessCameraProvider provider;
    private TextView banner;
    private View busy;
    private View permissionPanel;
    private ImageButton shutter;
    private ChipGroup fieldChips;
    private long fieldId;
    private boolean askedPermission;

    private final ActivityResultLauncher<String> askCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else showPermissionPanel(true);
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) runScan(null, uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        store = Store.of(c);
        FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(Color.BLACK);

        preview = new PreviewView(c);
        // TextureView-based preview behaves inside the pager while pages slide.
        preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        root.addView(preview, match());
        root.addView(new LeafFrameView(c), match());

        permissionPanel = buildPermissionPanel(c);
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        panelLp.leftMargin = panelLp.rightMargin = Ui.dp(c, 24);
        root.addView(permissionPanel, panelLp);
        permissionPanel.setVisibility(View.GONE);

        banner = Ui.text(c, "", Ui.Type.BODY);
        banner.setTextColor(Color.WHITE);
        banner.setPadding(Ui.dp(c, 16), Ui.dp(c, 12), Ui.dp(c, 16), Ui.dp(c, 12));
        banner.setBackground(Ui.rounded(Ui.color(c, R.color.mm_moderate), Ui.dp(c, 16)));
        FrameLayout.LayoutParams bannerLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        bannerLp.setMargins(Ui.dp(c, 16), Ui.dp(c, 12), Ui.dp(c, 16), 0);
        banner.setVisibility(View.GONE);
        banner.setOnClickListener(v -> v.setVisibility(View.GONE));
        root.addView(banner, bannerLp);

        LinearLayout controls = Ui.column(c);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);
        HorizontalScrollView chipScroll = new HorizontalScrollView(c);
        chipScroll.setHorizontalScrollBarEnabled(false);
        fieldChips = new ChipGroup(c);
        fieldChips.setSingleLine(true);
        fieldChips.setSingleSelection(true);
        fieldChips.setSelectionRequired(true);
        fieldChips.setPadding(Ui.dp(c, 16), 0, Ui.dp(c, 16), 0);
        chipScroll.addView(fieldChips);
        controls.addView(chipScroll);

        LinearLayout buttons = Ui.row(c);
        buttons.setGravity(Gravity.CENTER);
        buttons.setPadding(0, Ui.dp(c, 12), 0, 0);
        ImageButton gallery = roundButton(c, R.drawable.mm_ic_gallery, "Pick a photo", 52,
                Color.argb(90, 255, 255, 255), Color.WHITE);
        gallery.setOnClickListener(v -> pickPhoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        buttons.addView(gallery);
        View spaceLeft = new View(c);
        buttons.addView(spaceLeft, new LinearLayout.LayoutParams(Ui.dp(c, 28), 1));
        shutter = roundButton(c, R.drawable.mm_ic_scan, "Take photo", 76,
                Color.WHITE, Ui.color(c, R.color.mm_leaf));
        shutter.setOnClickListener(v -> takePhoto());
        buttons.addView(shutter);
        View spaceRight = new View(c);
        buttons.addView(spaceRight, new LinearLayout.LayoutParams(Ui.dp(c, 28 + 52), 1));
        controls.addView(buttons);

        FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        controlsLp.bottomMargin = Ui.dp(c, Ui.ISLAND_CLEARANCE_DP);
        root.addView(controls, controlsLp);

        busy = buildBusy(c);
        busy.setVisibility(View.GONE);
        root.addView(busy, match());
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        store.listen(getViewLifecycleOwner(), this::loadFields, Changes.Topic.CROPS);
        loadFields();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasCamera()) {
            showPermissionPanel(false);
            startCamera();
        } else if (!askedPermission) {
            askedPermission = true;
            askCamera.launch(Manifest.permission.CAMERA);
        } else {
            showPermissionPanel(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (provider != null) provider.unbindAll();
    }

    private boolean hasCamera() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        Context c = requireContext();
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(c);
        future.addListener(() -> {
            try {
                provider = future.get();
                bindCamera();
            } catch (Exception e) {
                showBanner("Camera is not available. You can still pick a photo.");
            }
        }, ContextCompat.getMainExecutor(c));
    }

    private void bindCamera() {
        if (!isResumed() || getView() == null) return;
        Preview p = new Preview.Builder().build();
        p.setSurfaceProvider(preview.getSurfaceProvider());
        capture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();
        provider.unbindAll();
        try {
            provider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, p, capture);
        } catch (IllegalArgumentException e) {
            capture = null;
            showBanner("No back camera found. You can still pick a photo.");
        }
    }

    private void takePhoto() {
        if (capture == null) {
            if (!hasCamera()) askCamera.launch(Manifest.permission.CAMERA);
            return;
        }
        File file = new File(requireContext().getCacheDir(), "scan_" + System.currentTimeMillis() + ".jpg");
        setBusy(true);
        capture.takePicture(new ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        runScan(file, null);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        setBusy(false);
                        showBanner("Could not take the photo. Try again.");
                    }
                });
    }

    /** Hands the photo to Store.scan; exactly one of file and uri is set. */
    private void runScan(@Nullable File file, @Nullable Uri uri) {
        if (getView() == null) return;
        setBusy(true);
        banner.setVisibility(View.GONE);
        Store.Done<ScanOutcome> done = outcome -> {
            setBusy(false);
            if (outcome.retake) showBanner(outcome.retakeReason);
            else Nav.of(this).openResult(outcome.scan.id);
        };
        Store.Done<Throwable> failed = t -> {
            setBusy(false);
            showBanner("Could not read that photo. Try another one.");
        };
        if (file != null) store.scan(getViewLifecycleOwner(), file, fieldId, done, failed);
        else store.scan(getViewLifecycleOwner(), uri, fieldId, done, failed);
    }

    private void loadFields() {
        store.load(getViewLifecycleOwner(), s -> s.crops().fields(), this::showFields);
    }

    private void showFields(List<Field> fields) {
        Context c = requireContext();
        fieldChips.removeAllViews();
        boolean found = false;
        fieldChips.addView(chip(c, "No field", 0));
        for (Field f : fields) {
            fieldChips.addView(chip(c, f.name + " · " + f.cropName, f.id));
            if (f.id == fieldId) found = true;
        }
        if (!found) fieldId = 0;
        for (int i = 0; i < fieldChips.getChildCount(); i++) {
            Chip chip = (Chip) fieldChips.getChildAt(i);
            if ((long) chip.getTag() == fieldId) chip.setChecked(true);
        }
        fieldChips.setVisibility(fields.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private Chip chip(Context c, String text, long id) {
        Chip chip = new Chip(c);
        chip.setText(text);
        chip.setTag(id);
        chip.setCheckable(true);
        chip.setOnCheckedChangeListener((b, checked) -> {
            if (checked) fieldId = id;
        });
        return chip;
    }

    private void setBusy(boolean on) {
        if (busy == null) return;
        busy.setVisibility(on ? View.VISIBLE : View.GONE);
        shutter.setEnabled(!on);
    }

    private void showBanner(String text) {
        if (banner == null) return;
        banner.setText(text);
        banner.setVisibility(View.VISIBLE);
    }

    private void showPermissionPanel(boolean show) {
        if (permissionPanel != null) permissionPanel.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private View buildPermissionPanel(Context c) {
        LinearLayout card = Ui.card(c);
        card.addView(Ui.text(c, "Camera needed", Ui.Type.HEADING));
        card.addView(Ui.gap(Ui.text(c,
                "DevPlant photographs leaves with the camera. Photos stay on this phone. "
                        + "You can also pick an existing photo.", Ui.Type.BODY), 6));
        View allow = Ui.gap(Ui.button(c, "Allow camera", true), 12);
        allow.setOnClickListener(v -> askCamera.launch(Manifest.permission.CAMERA));
        card.addView(allow);
        return card;
    }

    private View buildBusy(Context c) {
        FrameLayout scrim = new FrameLayout(c);
        scrim.setBackgroundColor(Ui.color(c, R.color.mm_scrim));
        scrim.setClickable(true);
        LinearLayout box = Ui.column(c);
        box.setGravity(Gravity.CENTER);
        box.addView(new ProgressBar(c));
        TextView t = Ui.text(c, "Checking the leaf…", Ui.Type.HEADING);
        t.setTextColor(Color.WHITE);
        box.addView(Ui.gap(t, 12));
        scrim.addView(box, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        return scrim;
    }

    private static ImageButton roundButton(Context c, int icon, String description, int sizeDp,
                                           int background, int tint) {
        ImageButton b = new ImageButton(c);
        b.setImageResource(icon);
        b.setImageTintList(android.content.res.ColorStateList.valueOf(tint));
        b.setContentDescription(description);
        b.setBackground(Ui.rounded(background, Ui.dp(c, sizeDp / 2f)));
        b.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(c, sizeDp), Ui.dp(c, sizeDp)));
        return b;
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
