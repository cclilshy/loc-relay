package com.cclilshy.locrelay.app;

import com.cclilshy.locrelay.event.runtime.EventNotificationListenerService;
import com.cclilshy.locrelay.event.permission.NotificationListenerConnectionPolicy;
import com.cclilshy.locrelay.event.permission.EventListenerPermissions;
import com.cclilshy.locrelay.event.permission.EventListenerPermissionSnapshot;
import com.cclilshy.locrelay.event.data.EventWebhookSubscriptionStore;
import com.cclilshy.locrelay.event.domain.EventListenerSubscriptionState;
import com.cclilshy.locrelay.event.domain.EventListenerSubscriptionPolicy;
import com.cclilshy.locrelay.webhook.runtime.WebhookDispatcher;
import com.cclilshy.locrelay.webhook.data.WebhookChannelStore;
import com.cclilshy.locrelay.webhook.domain.WebhookChannel;
import com.cclilshy.locrelay.qr.ui.QrScanActivity;
import com.cclilshy.locrelay.qr.domain.ServerScanPayload;
import com.cclilshy.locrelay.network.domain.NetworkInterfaceInfo;
import com.cclilshy.locrelay.network.android.NetworkInfoProvider;
import com.cclilshy.locrelay.proxy.domain.ProxyMapping;
import com.cclilshy.locrelay.frpc.data.FrpcProxyStore;
import com.cclilshy.locrelay.frpc.runtime.FrpcProcess;
import com.cclilshy.locrelay.gateway.runtime.GatewayRuntimeState;
import com.cclilshy.locrelay.gateway.runtime.GatewayService;
import com.cclilshy.locrelay.gateway.data.GatewayLogStore;
import com.cclilshy.locrelay.gateway.data.GatewayPrefs;
import com.cclilshy.locrelay.gateway.domain.GatewayStartupPolicy;
import com.cclilshy.locrelay.gateway.domain.GatewayExtensionSummary;
import com.cclilshy.locrelay.gateway.domain.GatewayExtensionCatalog;
import com.cclilshy.locrelay.gateway.domain.GatewayExtension;
import com.cclilshy.locrelay.common.permission.NotificationPermissionPolicy;
import com.cclilshy.locrelay.common.permission.AutoStartPermissionPolicy;
import com.cclilshy.locrelay.R;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.text.InputType;
import android.text.Layout;
import android.text.TextUtils;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MainActivity extends AppCompatActivity {
    private interface SettingSwitchBinder {
        void bind(SwitchMaterial toggle);
    }

    private interface SettingSwitchAction {
        void onToggle(SwitchMaterial toggle, boolean checked);
    }

    private static final int REQUEST_EVENT_CALL_PERMISSION = 3201;
    private static final int REQUEST_EVENT_SMS_PERMISSION = 3202;
    private static final int REQUEST_PERSISTENT_NOTIFICATION_PERMISSION = 3301;
    private static final int PENDING_EVENT_PERMISSION_NONE = 0;
    private static final int PENDING_EVENT_PERMISSION_CALL = 1;
    private static final int PENDING_EVENT_PERMISSION_SMS = 2;
    private static final int PENDING_EVENT_PERMISSION_NOTIFICATION = 3;
    private static final int PENDING_SETTING_PERMISSION_NONE = 0;
    private static final int PENDING_SETTING_PERMISSION_AUTO_START = 1;
    private static final int PENDING_SETTING_PERMISSION_PERSISTENT_NOTIFICATION = 2;
    private static final int PENDING_SETTING_PERMISSION_PERSISTENT_NOTIFICATION_AFTER_START = 3;
    private static final int COLOR_PRIMARY = 0xff006c67;
    private static final int COLOR_PRIMARY_CONTAINER = 0xffd8f3ef;
    private static final int COLOR_BACKGROUND = 0xfff7f9fc;
    private static final int COLOR_SURFACE = 0xffffffff;
    private static final int COLOR_SURFACE_VARIANT = 0xffeef3f7;
    private static final int COLOR_OUTLINE = 0xffd8dee8;
    private static final int COLOR_TEXT = 0xff172026;
    private static final int COLOR_MUTED = 0xff64717d;
    private static final int COLOR_ERROR_CONTAINER = 0xffffe2e2;
    private static final int COLOR_ERROR_TEXT = 0xff8a1c1c;
    private static final int COLOR_WARNING_CONTAINER = 0xfffff3c4;
    private static final int COLOR_WARNING_TEXT = 0xff6f4d00;
    private static final int TOOLBAR_ICON_BUTTON_DP = 40;
    private static final int INLINE_ICON_BUTTON_DP = 36;
    private static final int ICON_DP = 18;

    private LinearLayout mainShell;
    private MaterialToolbar toolbar;
    private FrameLayout contentContainer;
    private BottomNavigationView bottomNavigation;
    private int currentMenuId = R.id.menu_home;
    private String selectedExtensionId;
    private String selectedLogKey;
    private int logReturnMenuId = R.id.menu_home;
    private String logReturnExtensionId;
    private boolean selectedNetworkPage;
    private boolean syncingBottomSelection;

    private TextView statusText;
    private TextView homeSummaryText;
    private MaterialButton controlButton;
    private TextView serviceNotificationWarning;
    private TextView logTextView;
    private ScrollView logScrollView;
    private TextView networkSummary;
    private LinearLayout networkList;
    private SwitchMaterial extensionEnabledSwitch;
    private SwitchMaterial frpcPublishSwitch;
    private TextInputEditText frpcRemotePortInput;
    private LinearLayout frpcMappingList;
    private LinearLayout webhookChannelList;
    private IconActionButton serverScanButton;
    private IconActionButton addMappingButton;
    private IconActionButton addWebhookButton;
    private IconActionButton saveExtensionButton;
    private SwitchMaterial eventCallPermissionSwitch;
    private SwitchMaterial eventSmsPermissionSwitch;
    private SwitchMaterial eventNotificationPermissionSwitch;
    private SwitchMaterial autoStartSwitch;
    private SwitchMaterial startServiceWithAppSwitch;
    private SwitchMaterial persistentNotificationSwitch;
    private final List<CheckBox> eventChannelCheckboxes = new ArrayList<>();
    private int pendingEventPermission = PENDING_EVENT_PERMISSION_NONE;
    private String pendingEventSubscriptionKey;
    private int pendingSettingPermission = PENDING_SETTING_PERMISSION_NONE;
    private boolean pendingManualStartNotificationPermissionCheck;
    private final Map<String, TextInputEditText> extensionInputs = new HashMap<>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean contentRendered;
    private final SharedPreferences.OnSharedPreferenceChangeListener gatewayPrefsListener =
            (prefs, key) -> {
                if (GatewayPrefs.KEY_STATUS.equals(key) || GatewayPrefs.KEY_RUNNING.equals(key)) {
                    refreshStatus();
                    maybeRequestNotificationPermissionAfterManualStartIfReady();
                }
                if (GatewayPrefs.KEY_NO_WINDOW_MODE.equals(key)) {
                    applyNoWindowModePreference();
                }
                if (GatewayLogStore.isLogKey(key)) {
                    refreshLogText();
                }
            };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureSystemBars();
        setContentView(R.layout.activity_main);
        mainShell = findViewById(R.id.main_shell);
        toolbar = findViewById(R.id.top_app_bar);
        contentContainer = findViewById(R.id.content_container);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        styleBottomNavigation();
        applySystemBarInsets();
        setupNavigation();
        applyNoWindowModePreference();
        showPage(R.id.menu_home);
        maybeStartGatewayWithApp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GatewayPrefs.get(this).registerOnSharedPreferenceChangeListener(gatewayPrefsListener);
        applyNoWindowModePreference();
        boolean explicitPermissionReturn = pendingEventPermission != PENDING_EVENT_PERMISSION_NONE;
        boolean granted = !explicitPermissionReturn || isPendingEventPermissionGranted();
        if (explicitPermissionReturn && pendingEventSubscriptionKey != null) {
            GatewayPrefs.get(this).edit()
                    .putBoolean(pendingEventSubscriptionKey, granted)
                    .apply();
        }
        pendingEventPermission = PENDING_EVENT_PERMISSION_NONE;
        pendingEventSubscriptionKey = null;
        syncStartupSettingPermissions(explicitSettingPermissionReturn());
        syncEventListenerSubscriptions(explicitPermissionReturn && !granted);
        requestNotificationListenerRebindIfNeeded();
        refreshStatus();
    }

    @Override
    protected void onPause() {
        saveGatewayFormIfPresent(false);
        GatewayPrefs.get(this).unregisterOnSharedPreferenceChangeListener(gatewayPrefsListener);
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                applyScannedFrpcServer(result.getContents());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERSISTENT_NOTIFICATION_PERMISSION) {
            handlePersistentNotificationPermissionResult(
                    grantResults.length > 0
                            && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED);
            return;
        }
        if (requestCode != REQUEST_EVENT_CALL_PERMISSION && requestCode != REQUEST_EVENT_SMS_PERMISSION) {
            return;
        }
        boolean granted = grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (granted) {
            if (pendingEventSubscriptionKey != null) {
                GatewayPrefs.get(this).edit()
                        .putBoolean(pendingEventSubscriptionKey, true)
                        .apply();
            }
            pendingEventPermission = PENDING_EVENT_PERMISSION_NONE;
            pendingEventSubscriptionKey = null;
            syncEventListenerSubscriptions(false);
        } else {
            if (pendingEventSubscriptionKey != null) {
                GatewayPrefs.get(this).edit()
                        .putBoolean(pendingEventSubscriptionKey, false)
                        .apply();
            }
            syncEventListenerSubscriptions(false);
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            openAppPermissionSettings();
        }
    }

    @Override
    public void onBackPressed() {
        if (selectedLogKey != null) {
            restoreLogReturnPage();
            return;
        }
        if (selectedExtensionId != null) {
            selectedExtensionId = null;
            showPage(R.id.menu_extends);
            return;
        }
        if (selectedNetworkPage) {
            selectedNetworkPage = false;
            showPage(R.id.menu_setting);
            return;
        }
        super.onBackPressed();
    }

    private void configureSystemBars() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mainShell, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.LayoutParams toolbarParams = toolbar.getLayoutParams();
            toolbarParams.height = dp(64) + systemBars.top;
            toolbar.setLayoutParams(toolbarParams);
            toolbar.setPadding(toolbar.getPaddingLeft(), systemBars.top, toolbar.getPaddingRight(), 0);
            bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            bottomNavigation.setMinimumHeight(dp(72) + systemBars.bottom);
            return insets;
        });
    }

    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (syncingBottomSelection) {
                return true;
            }
            saveGatewayFormIfPresent(false);
            selectedLogKey = null;
            selectedExtensionId = null;
            selectedNetworkPage = false;
            showPage(item.getItemId());
            return true;
        });
    }

    private void styleBottomNavigation() {
        bottomNavigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        bottomNavigation.setItemRippleColor(rippleColor());
        bottomNavigation.setItemIconTintList(navItemColors());
        bottomNavigation.setItemTextColor(navItemColors());
        bottomNavigation.setItemActiveIndicatorEnabled(true);
        bottomNavigation.setItemActiveIndicatorColor(ColorStateList.valueOf(COLOR_PRIMARY_CONTAINER));
        bottomNavigation.setItemActiveIndicatorWidth(dp(72));
        bottomNavigation.setItemActiveIndicatorHeight(dp(34));
        bottomNavigation.setItemBackground(null);
        bottomNavigation.setElevation(0f);
    }

    private void showPage(int menuId) {
        selectedLogKey = null;
        currentMenuId = menuId;
        clearPageReferences();
        updateBottomNavigationVisibility();
        if (bottomNavigation.getSelectedItemId() != menuId) {
            syncingBottomSelection = true;
            bottomNavigation.setSelectedItemId(menuId);
            syncingBottomSelection = false;
        }
        if (menuId == R.id.menu_extends && selectedExtensionId != null) {
            GatewayExtension extension = GatewayExtensionCatalog.findById(selectedExtensionId);
            String logKey = toolbarLogKeyFor(menuId, selectedExtensionId);
            configureToolbar(
                    extension.getTitle(),
                    view -> {
                        saveGatewayFormIfPresent(false);
                        selectedExtensionId = null;
                        showPage(R.id.menu_extends);
                    },
                    logKey,
                    logPageTitleForExtension(extension.getId(), extension.getTitle()));
            replaceContent(buildExtensionDetailPage(extension));
        } else {
            if (menuId == R.id.menu_extends) {
                configureToolbar("Extends", null, toolbarLogKeyFor(menuId, null), "Gateway Logs");
                replaceContent(buildExtendsPage());
            } else if (menuId == R.id.menu_setting) {
                if (selectedNetworkPage) {
                    configureToolbar("Network", view -> {
                        selectedNetworkPage = false;
                        showPage(R.id.menu_setting);
                    }, null, null);
                    replaceContent(buildNetworkPage());
                } else {
                    configureToolbar("Setting", null, toolbarLogKeyFor(menuId, null), "Gateway Logs");
                    replaceContent(buildSettingPage());
                }
            } else {
                configureToolbar("Home", null, toolbarLogKeyFor(menuId, null), "Gateway Logs");
                replaceContent(buildHomePage());
            }
        }
        refreshStatus();
        refreshStatusSoon();
    }

    private void updateBottomNavigationVisibility() {
        setBottomNavigationVisible(shouldShowBottomNavigation(
                currentMenuId,
                selectedExtensionId,
                selectedNetworkPage,
                selectedLogKey));
    }

    private void setBottomNavigationVisible(boolean visible) {
        if (bottomNavigation == null) {
            return;
        }
        bottomNavigation.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void configureToolbar(String title, View.OnClickListener navigation, String logKey, String logTitle) {
        configureToolbar(title, navigation, logKey, logTitle, null);
    }

    private void configureToolbar(
            String title,
            View.OnClickListener navigation,
            String logKey,
            String logTitle,
            View.OnClickListener clearAction) {
        toolbar.setTitle("");
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);
        toolbar.getMenu().clear();
        toolbar.removeAllViews();
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setContentInsetStartWithNavigation(0);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(16), 0);

        if (navigation != null) {
            IconActionButton back = toolbarIconButton(R.drawable.ic_arrow_back_24, "Back");
            back.setOnClickListener(navigation);
            row.addView(back, squareParams(dp(TOOLBAR_ICON_BUTTON_DP)));
        }

        TextView titleView = text(title, 20, COLOR_TEXT, true);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1);
        titleParams.setMargins(navigation == null ? 0 : dp(8), 0, dp(8), 0);
        row.addView(titleView, titleParams);

        if (logKey != null) {
            IconActionButton logs = toolbarIconButton(R.drawable.ic_logs_24, "Logs");
            logs.setOnClickListener(view -> showLogPage(logKey, logTitle));
            row.addView(logs, squareParams(dp(TOOLBAR_ICON_BUTTON_DP)));
        }
        if (clearAction != null) {
            IconActionButton clear = toolbarIconButton(R.drawable.ic_delete_24, "Clear logs");
            clear.setOnClickListener(clearAction);
            row.addView(clear, squareParams(dp(TOOLBAR_ICON_BUTTON_DP)));
        }

        toolbar.addView(row, new Toolbar.LayoutParams(
                Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.MATCH_PARENT));
    }

    private static String toolbarLogKeyFor(int menuId, String selectedExtensionId) {
        if (menuId == R.id.menu_home) {
            return GatewayLogStore.KEY_TOTAL_LOG;
        }
        if (menuId == R.id.menu_extends && selectedExtensionId != null) {
            return GatewayLogStore.keyForExtension(selectedExtensionId);
        }
        return null;
    }

    private static String logPageTitleForExtension(String extensionId, String title) {
        return "event_listener".equals(extensionId) ? "Events" : title + " Logs";
    }

    private static int serviceIconFor(String extensionId) {
        if ("http".equals(extensionId)) {
            return R.drawable.ic_http_24;
        }
        if ("socks5".equals(extensionId)) {
            return R.drawable.ic_socks_24;
        }
        if ("frpc".equals(extensionId)) {
            return R.drawable.ic_frpc_24;
        }
        if ("webhook".equals(extensionId)) {
            return R.drawable.ic_extension_24;
        }
        if ("event_listener".equals(extensionId)) {
            return R.drawable.ic_event_listener_24;
        }
        return R.drawable.ic_extension_24;
    }

    private static boolean shouldShowBottomNavigation(
            int menuId,
            String selectedExtensionId,
            boolean selectedNetworkPage,
            String selectedLogKey) {
        if (selectedLogKey != null) {
            return false;
        }
        if (menuId == R.id.menu_extends && selectedExtensionId != null) {
            return false;
        }
        if (menuId == R.id.menu_setting && selectedNetworkPage) {
            return false;
        }
        return menuId == R.id.menu_home || menuId == R.id.menu_extends || menuId == R.id.menu_setting;
    }

    private boolean isGatewayEditable() {
        return !GatewayRuntimeState.isActive();
    }

    private void replaceContent(View nextPage) {
        if (contentRendered) {
            TransitionSet transition = new TransitionSet()
                    .addTransition(new Fade(Fade.OUT))
                    .addTransition(new Fade(Fade.IN))
                    .setDuration(140);
            TransitionManager.beginDelayedTransition(contentContainer, transition);
        }
        contentContainer.removeAllViews();
        contentContainer.addView(nextPage);
        contentRendered = true;
    }

    private View buildHomePage() {
        ScrollView scroll = pageScroll();
        scroll.setFillViewport(true);
        LinearLayout center = pageRoot();
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setPadding(dp(24), dp(28), dp(24), dp(28));

        TextView title = text("Loc Relay", 26, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        center.addView(title, bottomMarginParams(dp(8)));

        statusText = text("", 15, COLOR_PRIMARY, true);
        statusText.setGravity(Gravity.CENTER);
        center.addView(statusText, bottomMarginParams(dp(8)));

        homeSummaryText = text("", 14, COLOR_MUTED, false);
        homeSummaryText.setGravity(Gravity.CENTER);
        homeSummaryText.setSingleLine(false);
        center.addView(homeSummaryText, bottomMarginParams(dp(22)));

        controlButton = new MaterialButton(this);
        controlButton.setTextSize(18);
        controlButton.setTypeface(Typeface.DEFAULT_BOLD);
        controlButton.setOnClickListener(view -> toggleGateway());
        center.addView(controlButton, squareParams(dp(144)));

        serviceNotificationWarning = text(
                "Service notifications are off; background execution may be killed.\nTap to enable service notifications automatically.",
                13,
                COLOR_WARNING_TEXT,
                true);
        serviceNotificationWarning.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        serviceNotificationWarning.setPadding(dp(12), dp(10), dp(12), dp(10));
        serviceNotificationWarning.setBackground(sectionBackground(COLOR_WARNING_CONTAINER));
        serviceNotificationWarning.setOnClickListener(view -> requestServiceNotificationFromWarning());
        center.addView(serviceNotificationWarning, topMarginParams(dp(18)));

        scroll.addView(center, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT));
        return scroll;
    }

    private View buildExtendsPage() {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);

        root.addView(text("Services", 20, COLOR_TEXT, true));
        TextView summary = text(GatewayExtensionCatalog.all().size() + " services", 14, COLOR_MUTED, false);
        summary.setPadding(0, dp(6), 0, dp(8));
        root.addView(summary);

        SharedPreferences prefs = GatewayPrefs.get(this);
        for (GatewayExtension extension : GatewayExtensionCatalog.all()) {
            root.addView(extensionListCard(extension, prefs), compactSectionParams());
        }
        return scroll;
    }

    private MaterialCardView extensionListCard(GatewayExtension extension, SharedPreferences prefs) {
        MaterialCardView card = card(COLOR_SURFACE);
        card.setClickable(true);
        card.setOnClickListener(view -> {
            selectedExtensionId = extension.getId();
            showPage(R.id.menu_extends);
        });

        LinearLayout content = compactCardContent();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout serviceIcon = serviceIconBadge(
                serviceIconFor(extension.getId()),
                extension.getTitle() + " icon");
        LinearLayout.LayoutParams iconParams = squareParams(dp(44));
        iconParams.setMargins(0, 0, dp(12), 0);
        header.addView(serviceIcon, iconParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(extension.getTitle(), 16, COLOR_TEXT, true));
        TextView description = text(extension.getDescription(), 13, COLOR_MUTED, false);
        description.setPadding(0, dp(3), 0, 0);
        description.setSingleLine(true);
        description.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(description);

        GatewayExtensionSummary summary = extensionSummary(extension, prefs);
        SwitchMaterial serviceSwitch = serviceSwitch(summary.isEnabled());
        serviceSwitch.setContentDescription(extension.getTitle() + " service");
        serviceSwitch.setEnabled(isGatewayEditable());
        serviceSwitch.setOnClickListener(view -> {
            if (!isGatewayEditable()) {
                serviceSwitch.setChecked(!serviceSwitch.isChecked());
                Toast.makeText(this, "Stop gateway before editing services", Toast.LENGTH_SHORT).show();
                return;
            }
            GatewayPrefs.get(this).edit()
                    .putBoolean(extension.getEnabledPrefKey(), serviceSwitch.isChecked())
                    .apply();
        });
        header.addView(copy, weightWrapParams());
        header.addView(serviceSwitch, wrapParams());
        content.addView(header);

        ChipGroup chips = chipGroup();
        if (!extension.getBadgeLabel().isEmpty()) {
            chips.addView(chip(extension.getBadgeLabel(), COLOR_PRIMARY, COLOR_PRIMARY_CONTAINER));
        }
        chips.addView(chip(summary.getEndpointSummary(), COLOR_MUTED, COLOR_SURFACE_VARIANT));
        content.addView(chips, topMarginParams(dp(8)));

        card.addView(content);
        return card;
    }

    private View buildExtensionDetailPage(GatewayExtension extension) {
        if ("frpc".equals(extension.getId())) {
            return buildFrpcDetailPage(extension);
        }
        if ("webhook".equals(extension.getId())) {
            return buildWebhookDetailPage(extension);
        }
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);
        SharedPreferences prefs = GatewayPrefs.get(this);
        root.addView(buildExpandableServiceSection(
                extension,
                prefs,
                extension.getTitle(),
                extensionSummary(extension, prefs).getEndpointSummary(),
                false), sectionParams());

        saveExtensionButton = iconButton(R.drawable.ic_check_24, "Save", true);
        saveExtensionButton.setOnClickListener(view -> saveGatewayFormAndReturn());
        root.addView(actionRow(saveExtensionButton), topMarginParams(dp(14)));
        return scroll;
    }

    private View buildFrpcDetailPage(GatewayExtension extension) {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);

        SharedPreferences prefs = GatewayPrefs.get(this);
        root.addView(buildExpandableServiceSection(
                extension,
                prefs,
                "Server",
                frpcServerSummary(prefs),
                true), sectionParams());

        LinearLayout listHeader = new LinearLayout(this);
        listHeader.setOrientation(LinearLayout.HORIZONTAL);
        listHeader.setGravity(Gravity.CENTER_VERTICAL);
        listHeader.addView(text("Proxy mappings", 18, COLOR_TEXT, true), weightWrapParams());
        addMappingButton = iconButton(R.drawable.ic_add_24, "Add mapping", false);
        addMappingButton.setOnClickListener(view -> showFrpcMappingSheet(null));
        listHeader.addView(addMappingButton, wrapParams());
        root.addView(listHeader, fullWidthBottomMarginParams(dp(8)));

        frpcMappingList = new LinearLayout(this);
        frpcMappingList.setOrientation(LinearLayout.VERTICAL);
        root.addView(frpcMappingList);
        refreshFrpcMappingList();

        saveExtensionButton = iconButton(R.drawable.ic_check_24, "Save", true);
        saveExtensionButton.setOnClickListener(view -> saveGatewayFormAndReturn());
        root.addView(actionRow(saveExtensionButton), topMarginParams(dp(14)));
        return scroll;
    }

    private View buildWebhookDetailPage(GatewayExtension extension) {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);

        SharedPreferences prefs = GatewayPrefs.get(this);
        root.addView(buildExpandableServiceSection(
                extension,
                prefs,
                "Channels",
                webhookSummary(prefs),
                false), sectionParams());

        LinearLayout listHeader = new LinearLayout(this);
        listHeader.setOrientation(LinearLayout.HORIZONTAL);
        listHeader.setGravity(Gravity.CENTER_VERTICAL);
        listHeader.addView(text("WebHook channels", 18, COLOR_TEXT, true), weightWrapParams());
        addWebhookButton = iconButton(R.drawable.ic_add_24, "Add WebHook channel", false);
        addWebhookButton.setOnClickListener(view -> showWebhookChannelSheet(null));
        listHeader.addView(addWebhookButton, wrapParams());
        root.addView(listHeader, fullWidthBottomMarginParams(dp(8)));

        webhookChannelList = new LinearLayout(this);
        webhookChannelList.setOrientation(LinearLayout.VERTICAL);
        root.addView(webhookChannelList);
        refreshWebhookChannelList();
        return scroll;
    }

    private void refreshWebhookChannelList() {
        if (webhookChannelList == null) {
            return;
        }
        webhookChannelList.removeAllViews();
        List<WebhookChannel> channels = WebhookChannelStore.parse(GatewayPrefs.getString(
                GatewayPrefs.get(this),
                GatewayPrefs.KEY_WEBHOOK_CHANNELS,
                ""));
        if (channels.isEmpty()) {
            TextView empty = text("No WebHook channels", 14, COLOR_MUTED, false);
            empty.setPadding(0, dp(10), 0, dp(4));
            webhookChannelList.addView(empty);
            return;
        }
        boolean canEdit = isGatewayEditable();
        for (WebhookChannel channel : channels) {
            webhookChannelList.addView(webhookChannelCard(channel, canEdit), compactSectionParams());
        }
    }

    private MaterialCardView webhookChannelCard(WebhookChannel channel, boolean canEdit) {
        MaterialCardView card = card(COLOR_SURFACE);
        card.setClickable(canEdit);
        if (canEdit) {
            card.setOnClickListener(view -> showWebhookChannelSheet(channel));
        }
        LinearLayout content = compactCardContent();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(channel.getName(), 16, COLOR_TEXT, true));
        TextView target = text(channel.getTargetUrl(), 13, COLOR_MUTED, false);
        target.setPadding(0, dp(3), 0, 0);
        target.setSingleLine(true);
        target.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(target);
        if (!channel.getProxyUrl().isEmpty()) {
            TextView proxy = text("proxy " + channel.getProxyUrl(), 12, COLOR_MUTED, false);
            proxy.setPadding(0, dp(2), 0, 0);
            proxy.setSingleLine(true);
            proxy.setEllipsize(TextUtils.TruncateAt.END);
            copy.addView(proxy);
        }
        row.addView(copy, weightWrapParams());

        IconActionButton removeButton = iconButton(
                R.drawable.ic_delete_24,
                canEdit ? "Remove WebHook channel" : "Remove WebHook channel disabled while running",
                false);
        removeButton.setEnabled(canEdit);
        styleTonalIconButton(removeButton, canEdit);
        if (canEdit) {
            removeButton.setOnClickListener(view -> confirmRemoveWebhookChannel(channel));
        }
        row.addView(removeButton, wrapParams());
        content.addView(row);
        card.addView(content);
        return card;
    }

    private void showWebhookChannelSheet(WebhookChannel channel) {
        if (!isGatewayEditable()) {
            Toast.makeText(this, "Stop gateway before editing WebHook channels", Toast.LENGTH_SHORT).show();
            refreshWebhookChannelList();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout root = pageRoot();
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.addView(text(channel == null ? "Add WebHook" : "Edit WebHook", 20, COLOR_TEXT, true));
        TextInputEditText name = addInput(root, "Name", GatewayExtension.FieldType.TEXT);
        TextInputEditText targetUrl = addInput(root, "Target URL", GatewayExtension.FieldType.TEXT);
        TextInputEditText proxyUrl = addInput(root, "Proxy URL", GatewayExtension.FieldType.TEXT);
        name.setText(channel == null ? "" : channel.getName());
        targetUrl.setText(channel == null ? "" : channel.getTargetUrl());
        proxyUrl.setText(channel == null ? "" : channel.getProxyUrl());

        IconActionButton save = iconButton(R.drawable.ic_check_24, "Save WebHook", true);
        save.setOnClickListener(view -> saveWebhookChannelFromSheet(dialog, channel, name, targetUrl, proxyUrl));
        root.addView(actionRow(save), topMarginParams(dp(16)));
        dialog.setContentView(root);
        dialog.show();
    }

    private void saveWebhookChannelFromSheet(
            BottomSheetDialog dialog,
            WebhookChannel previous,
            TextInputEditText name,
            TextInputEditText targetUrl,
            TextInputEditText proxyUrl) {
        if (!isGatewayEditable()) {
            Toast.makeText(this, "Stop gateway before saving WebHook channels", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            refreshWebhookChannelList();
            return;
        }
        try {
            WebhookChannel channel = new WebhookChannel(
                    valueOf(name),
                    valueOf(targetUrl),
                    valueOf(proxyUrl));
            SharedPreferences prefs = GatewayPrefs.get(this);
            String stored = GatewayPrefs.getString(prefs, GatewayPrefs.KEY_WEBHOOK_CHANNELS, "");
            if (previous != null && !previous.getName().equals(channel.getName())) {
                stored = WebhookChannelStore.remove(stored, previous.getName());
            }
            stored = WebhookChannelStore.serialize(WebhookChannelStore.upsert(stored, channel));
            prefs.edit().putString(GatewayPrefs.KEY_WEBHOOK_CHANNELS, stored).apply();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            refreshWebhookChannelList();
        } catch (IllegalArgumentException err) {
            Toast.makeText(this, "Invalid WebHook", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmRemoveWebhookChannel(WebhookChannel channel) {
        if (!isGatewayEditable()) {
            Toast.makeText(this, "Stop gateway before removing WebHook channels", Toast.LENGTH_SHORT).show();
            refreshWebhookChannelList();
            return;
        }
        GatewayPrefs.get(this).edit()
                .putString(
                        GatewayPrefs.KEY_WEBHOOK_CHANNELS,
                        WebhookChannelStore.remove(
                                GatewayPrefs.getString(
                                        GatewayPrefs.get(this),
                                        GatewayPrefs.KEY_WEBHOOK_CHANNELS,
                                        ""),
                                channel.getName()))
                .apply();
        Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
        refreshWebhookChannelList();
    }

    private MaterialCardView buildExpandableServiceSection(
            GatewayExtension extension,
            SharedPreferences prefs,
            String title,
            String summaryText,
            boolean includeServerScan) {
        MaterialCardView card = card(COLOR_SURFACE);
        LinearLayout content = compactCardContent();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title, 17, COLOR_TEXT, true));
        TextView summary = text(summaryText, 13, COLOR_MUTED, false);
        summary.setPadding(0, dp(3), 0, 0);
        summary.setSingleLine(true);
        summary.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(summary);
        header.addView(copy, weightWrapParams());
        if (includeServerScan) {
            serverScanButton = iconButton(R.drawable.ic_qr_scan_24, "Scan server QR", false);
            serverScanButton.setOnClickListener(view -> scanFrpcServerQr());
            LinearLayout.LayoutParams scanParams = wrapParams();
            scanParams.setMargins(0, 0, dp(8), 0);
            header.addView(serverScanButton, scanParams);
        }
        IconActionButton toggle = iconButton(R.drawable.ic_expand_more_24, "Expand " + title.toLowerCase(), false);
        header.addView(toggle, wrapParams());
        content.addView(header);

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setVisibility(View.GONE);

        extensionInputs.clear();
        for (GatewayExtension.Field field : extension.getFields()) {
            TextInputEditText input = addInput(fields, field.getLabel(), field.getType());
            input.setText(GatewayPrefs.getString(prefs, field.getPrefKey(), field.getDefaultValue()));
            extensionInputs.put(field.getPrefKey(), input);
        }
        if (isBuiltInProxy(extension.getId())) {
            addBuiltInFrpcSection(fields, extension.getId(), prefs);
        }
        if ("event_listener".equals(extension.getId())) {
            addEventListenerPermissionSection(fields);
        }
        content.addView(fields);

        View.OnClickListener toggleExpansion = view -> {
            boolean expand = fields.getVisibility() != View.VISIBLE;
            fields.setVisibility(expand ? View.VISIBLE : View.GONE);
            toggle.setIconResource(expand ? R.drawable.ic_expand_less_24 : R.drawable.ic_expand_more_24);
            toggle.setContentDescription(expand ? "Collapse " + title.toLowerCase() : "Expand " + title.toLowerCase());
        };
        header.setOnClickListener(toggleExpansion);
        toggle.setOnClickListener(toggleExpansion);

        card.addView(content);
        return card;
    }

    private void scanFrpcServerQr() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(QrScanActivity.class);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan server QR");
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    private void applyScannedFrpcServer(String value) {
        try {
            ServerScanPayload payload = ServerScanPayload.parse(value);
            boolean wasActive = GatewayRuntimeState.isActive();
            GatewayPrefs.get(this).edit()
                    .putBoolean(GatewayPrefs.KEY_FRPC_ENABLED, true)
                    .putString(GatewayPrefs.KEY_FRPC_SERVER, payload.getServer())
                    .putString(GatewayPrefs.KEY_FRPC_SERVER_PORT, Integer.toString(payload.getPort()))
                    .putString(GatewayPrefs.KEY_FRPC_TOKEN, payload.getToken())
                    .apply();
            if ("frpc".equals(selectedExtensionId)) {
                showPage(R.id.menu_extends);
            }
            if (wasActive) {
                startGateway(false, false);
                Toast.makeText(this, "Server saved and gateway restarted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Server saved", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalArgumentException err) {
            Toast.makeText(this, "Invalid server QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void addBuiltInFrpcSection(LinearLayout content, String extensionId, SharedPreferences prefs) {
        TextView sectionTitle = text("FRPC exposure", 15, COLOR_TEXT, true);
        sectionTitle.setPadding(0, dp(18), 0, 0);
        content.addView(sectionTitle);

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        switchRow.setPadding(0, dp(10), 0, dp(4));
        switchRow.addView(text("Publish", 15, COLOR_TEXT, true), weightWrapParams());
        frpcPublishSwitch = serviceSwitch(FrpcProxyStore.hasMapping(prefs, extensionId));
        frpcPublishSwitch.setContentDescription(extensionId + " FRPC exposure");
        switchRow.addView(frpcPublishSwitch, wrapParams());
        content.addView(switchRow);

        frpcRemotePortInput = addInput(content, "Remote port", GatewayExtension.FieldType.NUMBER);
        frpcRemotePortInput.setText(remotePortValueFor(extensionId, prefs));
        frpcPublishSwitch.setOnCheckedChangeListener((button, checked) -> updateFrpcRemotePortEnabled());
        updateFrpcRemotePortEnabled();
    }

    private void refreshFrpcMappingList() {
        if (frpcMappingList == null) {
            return;
        }
        frpcMappingList.removeAllViews();
        List<ProxyMapping> mappings = safeParseMappings(GatewayPrefs.getString(
                GatewayPrefs.get(this),
                GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS,
                ""));
        if (mappings.isEmpty()) {
            TextView empty = text("No mappings", 14, COLOR_MUTED, false);
            empty.setPadding(0, dp(10), 0, dp(4));
            frpcMappingList.addView(empty);
            return;
        }
        boolean canEdit = isGatewayEditable();
        for (ProxyMapping mapping : mappings) {
            frpcMappingList.addView(frpcMappingCard(mapping, canEdit), compactSectionParams());
        }
    }

    private MaterialCardView frpcMappingCard(ProxyMapping mapping, boolean canEdit) {
        MaterialCardView card = card(COLOR_SURFACE);
        card.setClickable(canEdit);
        if (canEdit) {
            card.setOnClickListener(view -> showFrpcMappingSheet(mapping));
        }
        LinearLayout content = compactCardContent();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(mapping.getName(), 16, COLOR_TEXT, true));
        TextView summary = text(mapping.getType() + "  " + mapping.getLocalIp() + ":"
                + mapping.getLocalPort() + " -> " + mapping.getRemotePort(), 13, COLOR_MUTED, false);
        summary.setPadding(0, dp(3), 0, 0);
        copy.addView(summary);
        row.addView(copy, weightWrapParams());

        IconActionButton removeButton = iconButton(
                R.drawable.ic_delete_24,
                canEdit ? "Remove mapping" : "Remove mapping disabled while running",
                false);
        removeButton.setEnabled(canEdit);
        styleTonalIconButton(removeButton, canEdit);
        if (canEdit) {
            removeButton.setOnClickListener(view -> confirmRemoveFrpcMapping(mapping));
        }
        row.addView(removeButton, wrapParams());
        content.addView(row);
        card.addView(content);
        return card;
    }

    private void confirmRemoveFrpcMapping(ProxyMapping mapping) {
        if (!isGatewayEditable()) {
            Toast.makeText(this, "Stop gateway before removing mappings", Toast.LENGTH_SHORT).show();
            refreshFrpcMappingList();
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout root = pageRoot();
        root.setPadding(dp(22), dp(20), dp(22), dp(18));
        root.addView(text("Remove mapping?", 20, COLOR_TEXT, true));
        TextView message = text(mapping.getName(), 14, COLOR_MUTED, false);
        message.setPadding(0, dp(8), 0, dp(12));
        root.addView(message);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        IconActionButton cancel = iconButton(R.drawable.ic_close_24, "Cancel", false);
        cancel.setOnClickListener(view -> dialog.dismiss());
        IconActionButton remove = iconButton(R.drawable.ic_delete_24, "Remove mapping", true);
        remove.setOnClickListener(view -> {
            FrpcProxyStore.remove(GatewayPrefs.get(this), mapping.getName());
            Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            refreshFrpcMappingList();
        });
        LinearLayout.LayoutParams cancelParams = wrapParams();
        cancelParams.setMargins(0, 0, dp(10), 0);
        actions.addView(cancel, cancelParams);
        actions.addView(remove, wrapParams());
        root.addView(actions, topMarginParams(dp(6)));

        dialog.setView(root);
        dialog.show();
    }

    private void showFrpcMappingSheet(ProxyMapping mapping) {
        if (!isGatewayEditable()) {
            Toast.makeText(this, "Stop gateway before editing mappings", Toast.LENGTH_SHORT).show();
            refreshFrpcMappingList();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout root = pageRoot();
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.addView(text(mapping == null ? "Add mapping" : "Edit mapping", 20, COLOR_TEXT, true));
        TextInputEditText name = addInput(root, "Name", GatewayExtension.FieldType.TEXT);
        TextInputEditText type = addInput(root, "Type", GatewayExtension.FieldType.TEXT);
        TextInputEditText localIp = addInput(root, "Local IP", GatewayExtension.FieldType.TEXT);
        TextInputEditText localPort = addInput(root, "Local port", GatewayExtension.FieldType.NUMBER);
        TextInputEditText remotePort = addInput(root, "Remote port", GatewayExtension.FieldType.NUMBER);
        name.setText(mapping == null ? "" : mapping.getName());
        type.setText(mapping == null ? "tcp" : mapping.getType());
        localIp.setText(mapping == null ? "127.0.0.1" : mapping.getLocalIp());
        localPort.setText(mapping == null ? "" : Integer.toString(mapping.getLocalPort()));
        remotePort.setText(mapping == null ? "" : Integer.toString(mapping.getRemotePort()));

        IconActionButton save = iconButton(R.drawable.ic_check_24, "Save mapping", true);
        save.setOnClickListener(view -> saveFrpcMappingFromSheet(dialog, mapping, name, type, localIp, localPort, remotePort));
        root.addView(actionRow(save), topMarginParams(dp(16)));
        dialog.setContentView(root);
        dialog.show();
    }

    private void saveFrpcMappingFromSheet(
            BottomSheetDialog dialog,
            ProxyMapping previous,
            TextInputEditText name,
            TextInputEditText type,
            TextInputEditText localIp,
            TextInputEditText localPort,
            TextInputEditText remotePort) {
        if (!isGatewayEditable()) {
            Toast.makeText(this, "Stop gateway before saving mappings", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            refreshFrpcMappingList();
            return;
        }
        int parsedLocalPort = parsePortOrDefault(valueOf(localPort), 0);
        int parsedRemotePort = parsePortOrDefault(valueOf(remotePort), 0);
        try {
            ProxyMapping next = new ProxyMapping(
                    valueOf(name).trim(),
                    valueOf(type).trim(),
                    valueOf(localIp).trim(),
                    parsedLocalPort,
                    parsedRemotePort);
            SharedPreferences prefs = GatewayPrefs.get(this);
            String mappings = GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS, "");
            if (previous != null && !previous.getName().equals(next.getName())) {
                mappings = FrpcProxyStore.remove(mappings, previous.getName());
            }
            mappings = FrpcProxyStore.upsert(mappings, next);
            prefs.edit().putString(GatewayPrefs.KEY_FRPC_PROXY_MAPPINGS, mappings).apply();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            refreshFrpcMappingList();
        } catch (IllegalArgumentException err) {
            Toast.makeText(this, "Invalid mapping", Toast.LENGTH_SHORT).show();
        }
    }

    private String frpcServerSummary(SharedPreferences prefs) {
        String server = GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_SERVER, "");
        String port = GatewayPrefs.getString(
                prefs,
                GatewayPrefs.KEY_FRPC_SERVER_PORT,
                Integer.toString(GatewayPrefs.DEFAULT_FRPC_SERVER_PORT));
        return server.trim().isEmpty() ? "not configured" : server + ":" + port;
    }

    private String webhookSummary(SharedPreferences prefs) {
        int count = WebhookChannelStore.parse(GatewayPrefs.getString(
                prefs,
                GatewayPrefs.KEY_WEBHOOK_CHANNELS,
                "")).size();
        return count == 0 ? "no channels" : count + " channels";
    }

    private void addEventListenerPermissionSection(LinearLayout content) {
        TextView sectionTitle = text("Events", 15, COLOR_TEXT, true);
        sectionTitle.setPadding(0, dp(18), 0, dp(8));
        content.addView(sectionTitle);

        EventListenerSubscriptionState state = syncEventListenerSubscriptions(false);
        eventCallPermissionSwitch = eventListenerEventCard(
                content,
                "Call events",
                "call",
                state.isCallEnabled(),
                GatewayPrefs.KEY_EVENT_CALL_ENABLED,
                REQUEST_EVENT_CALL_PERMISSION);
        eventSmsPermissionSwitch = eventListenerEventCard(
                content,
                "SMS events",
                "sms",
                state.isSmsEnabled(),
                GatewayPrefs.KEY_EVENT_SMS_ENABLED,
                REQUEST_EVENT_SMS_PERMISSION);
        eventNotificationPermissionSwitch = eventListenerEventCard(
                content,
                "App notifications",
                "notification",
                state.isNotificationEnabled(),
                GatewayPrefs.KEY_EVENT_NOTIFICATION_ENABLED,
                PENDING_EVENT_PERMISSION_NOTIFICATION);
    }

    private SwitchMaterial eventListenerEventCard(
            LinearLayout content,
            String label,
            String eventType,
            boolean enabled,
            String prefKey,
            int requestCode) {
        MaterialCardView card = card(COLOR_SURFACE_VARIANT);
        LinearLayout root = compactCardContent();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text(label, 14, COLOR_TEXT, true), weightWrapParams());

        SwitchMaterial toggle = serviceSwitch(enabled);
        toggle.setContentDescription(label);
        toggle.setEnabled(isGatewayEditable());
        toggle.setOnClickListener(view -> setEventSubscription(toggle, prefKey, requestCode, toggle.isChecked()));
        header.addView(toggle, wrapParams());

        IconActionButton expand = iconButton(R.drawable.ic_expand_more_24, "Expand " + label.toLowerCase(), false);
        LinearLayout.LayoutParams expandParams = wrapParams();
        expandParams.setMargins(dp(8), 0, 0, 0);
        header.addView(expand, expandParams);
        root.addView(header);

        LinearLayout channels = new LinearLayout(this);
        channels.setOrientation(LinearLayout.VERTICAL);
        channels.setVisibility(View.GONE);
        addWebhookChannelChoices(channels, eventType);
        root.addView(channels, topMarginParams(dp(10)));

        View.OnClickListener toggleExpansion = view -> {
            boolean show = channels.getVisibility() != View.VISIBLE;
            channels.setVisibility(show ? View.VISIBLE : View.GONE);
            expand.setIconResource(show ? R.drawable.ic_expand_less_24 : R.drawable.ic_expand_more_24);
            expand.setContentDescription(show ? "Collapse " + label.toLowerCase() : "Expand " + label.toLowerCase());
        };
        header.setOnClickListener(toggleExpansion);
        expand.setOnClickListener(toggleExpansion);
        card.addView(root);
        content.addView(card, topMarginParams(dp(8)));
        return toggle;
    }

    private void addWebhookChannelChoices(LinearLayout content, String eventType) {
        TextView label = text("Channels", 13, COLOR_MUTED, true);
        label.setPadding(0, 0, 0, dp(4));
        content.addView(label);
        SharedPreferences prefs = GatewayPrefs.get(this);
        List<WebhookChannel> channels = WebhookChannelStore.parse(GatewayPrefs.getString(
                prefs,
                GatewayPrefs.KEY_WEBHOOK_CHANNELS,
                ""));
        if (channels.isEmpty()) {
            TextView empty = text("No WebHook channels", 13, COLOR_MUTED, false);
            empty.setPadding(0, dp(4), 0, 0);
            content.addView(empty);
            return;
        }
        String prefKey = WebhookDispatcher.subscriptionKey(eventType);
        String selected = GatewayPrefs.getString(prefs, prefKey, "");
        boolean editable = isGatewayEditable();
        if (!prefs.getBoolean(GatewayPrefs.KEY_WEBHOOK_ENABLED, false)) {
            TextView disabled = text("WebHook extension is off", 12, COLOR_MUTED, false);
            disabled.setPadding(0, 0, 0, dp(4));
            content.addView(disabled);
        }
        for (WebhookChannel channel : channels) {
            CheckBox checkbox = new CheckBox(this);
            checkbox.setText(channel.getName());
            checkbox.setTextColor(COLOR_TEXT);
            checkbox.setTextSize(14);
            checkbox.setChecked(EventWebhookSubscriptionStore.isSelected(selected, channel.getName()));
            checkbox.setEnabled(editable);
            checkbox.setOnCheckedChangeListener((button, checked) -> {
                if (!isGatewayEditable()) {
                    Toast.makeText(this, "Stop gateway before editing listener channels", Toast.LENGTH_SHORT).show();
                    return;
                }
                String current = GatewayPrefs.getString(GatewayPrefs.get(this), prefKey, "");
                GatewayPrefs.get(this).edit()
                        .putString(prefKey, EventWebhookSubscriptionStore.setSelected(
                                current,
                                channel.getName(),
                                checked))
                        .apply();
            });
            eventChannelCheckboxes.add(checkbox);
            content.addView(checkbox);
        }
    }

    private void setEventSubscription(SwitchMaterial toggle, String prefKey, int requestCode, boolean enabled) {
        if (!isGatewayEditable()) {
            toggle.setChecked(!enabled);
            Toast.makeText(this, "Stop gateway before editing listener events", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!enabled) {
            GatewayPrefs.get(this).edit().putBoolean(prefKey, false).apply();
            toggle.setChecked(false);
            syncEventListenerSubscriptions(false);
            return;
        }
        if (eventPermissionGranted(requestCode)) {
            GatewayPrefs.get(this).edit().putBoolean(prefKey, true).apply();
            toggle.setChecked(true);
            syncEventListenerSubscriptions(false);
            return;
        }
        GatewayPrefs.get(this).edit().putBoolean(prefKey, false).apply();
        toggle.setChecked(false);
        requestEventListenerPermission(requestCode, prefKey);
    }

    private boolean eventPermissionGranted(int requestCode) {
        EventListenerPermissionSnapshot snapshot = EventListenerPermissions.snapshot(this);
        if (requestCode == REQUEST_EVENT_CALL_PERMISSION) {
            return snapshot.isCallGranted();
        }
        if (requestCode == REQUEST_EVENT_SMS_PERMISSION) {
            return snapshot.isSmsGranted();
        }
        return snapshot.isNotificationGranted();
    }

    private void requestEventListenerPermission(int requestCode, String prefKey) {
        pendingEventSubscriptionKey = prefKey;
        if (requestCode == REQUEST_EVENT_CALL_PERMISSION) {
            pendingEventPermission = PENDING_EVENT_PERMISSION_CALL;
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_EVENT_CALL_PERMISSION);
            return;
        }
        if (requestCode == REQUEST_EVENT_SMS_PERMISSION) {
            pendingEventPermission = PENDING_EVENT_PERMISSION_SMS;
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECEIVE_SMS},
                    REQUEST_EVENT_SMS_PERMISSION);
            return;
        }
        pendingEventPermission = PENDING_EVENT_PERMISSION_NOTIFICATION;
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        Toast.makeText(this, "Enable notification access for Loc Relay", Toast.LENGTH_SHORT).show();
    }

    private boolean isPendingEventPermissionGranted() {
        EventListenerPermissionSnapshot snapshot = EventListenerPermissions.snapshot(this);
        if (pendingEventPermission == PENDING_EVENT_PERMISSION_CALL) {
            return snapshot.isCallGranted();
        }
        if (pendingEventPermission == PENDING_EVENT_PERMISSION_SMS) {
            return snapshot.isSmsGranted();
        }
        if (pendingEventPermission == PENDING_EVENT_PERMISSION_NOTIFICATION) {
            return snapshot.isNotificationGranted();
        }
        return true;
    }

    private EventListenerSubscriptionState syncEventListenerSubscriptions(boolean showDeniedMessage) {
        SharedPreferences prefs = GatewayPrefs.get(this);
        EventListenerSubscriptionState state = EventListenerSubscriptionPolicy.sync(
                prefs.getBoolean(GatewayPrefs.KEY_EVENT_CALL_ENABLED, false),
                prefs.getBoolean(GatewayPrefs.KEY_EVENT_SMS_ENABLED, false),
                prefs.getBoolean(GatewayPrefs.KEY_EVENT_NOTIFICATION_ENABLED, false),
                EventListenerPermissions.snapshot(this));
        prefs.edit()
                .putBoolean(GatewayPrefs.KEY_EVENT_CALL_ENABLED, state.isCallEnabled())
                .putBoolean(GatewayPrefs.KEY_EVENT_SMS_ENABLED, state.isSmsEnabled())
                .putBoolean(GatewayPrefs.KEY_EVENT_NOTIFICATION_ENABLED, state.isNotificationEnabled())
                .apply();
        if (eventCallPermissionSwitch == null
                && eventSmsPermissionSwitch == null
                && eventNotificationPermissionSwitch == null) {
            if (showDeniedMessage) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
            return state;
        }
        if (eventCallPermissionSwitch != null) {
            eventCallPermissionSwitch.setChecked(state.isCallEnabled());
        }
        if (eventSmsPermissionSwitch != null) {
            eventSmsPermissionSwitch.setChecked(state.isSmsEnabled());
        }
        if (eventNotificationPermissionSwitch != null) {
            eventNotificationPermissionSwitch.setChecked(state.isNotificationEnabled());
        }
        if (showDeniedMessage) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
        return state;
    }

    private void requestNotificationListenerRebindIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        SharedPreferences prefs = GatewayPrefs.get(this);
        if (!NotificationListenerConnectionPolicy.shouldRequestRebind(
                EventListenerPermissions.snapshot(this).isNotificationGranted(),
                prefs.getBoolean(GatewayPrefs.KEY_EVENT_NOTIFICATION_ENABLED, false),
                prefs.getBoolean(GatewayPrefs.KEY_RUNNING, false),
                prefs.getBoolean(GatewayPrefs.KEY_EVENT_LISTENER_ENABLED, false))) {
            return;
        }
        NotificationListenerService.requestRebind(
                new ComponentName(this, EventNotificationListenerService.class));
    }

    private void openAppPermissionSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private View buildNetworkPage() {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);

        TextView title = text("Local network", 20, COLOR_TEXT, true);
        root.addView(title);
        networkSummary = text("", 14, COLOR_MUTED, false);
        networkSummary.setPadding(0, dp(6), 0, dp(8));
        root.addView(networkSummary);

        networkList = new LinearLayout(this);
        networkList.setOrientation(LinearLayout.VERTICAL);
        root.addView(networkList, topMarginParams(dp(8)));

        refreshNetworkInfo();
        return scroll;
    }

    private View buildSettingPage() {
        ScrollView scroll = pageScroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);
        SharedPreferences prefs = GatewayPrefs.get(this);

        TextView section = text("General", 13, COLOR_MUTED, true);
        section.setPadding(dp(4), dp(4), dp(4), dp(8));
        root.addView(section);

        MaterialCardView group = card(COLOR_SURFACE);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(settingItem(
                R.drawable.ic_network_24,
                "network",
                "Local interfaces, addresses, and connection state",
                view -> {
                    selectedNetworkPage = true;
                    showPage(R.id.menu_setting);
                }));
        content.addView(settingGuardedSwitchItem(
                R.drawable.ic_settings_24,
                "Auto start",
                "Start gateway after device boot",
                prefs.getBoolean(
                        GatewayPrefs.KEY_AUTO_START_ENABLED,
                        GatewayStartupPolicy.isAutoStartEnabledByDefault()),
                toggle -> autoStartSwitch = toggle,
                (toggle, checked) -> setAutoStartEnabled(toggle, checked)));
        content.addView(settingSwitchItem(
                R.drawable.ic_home_24,
                "Start service with app",
                "Start gateway when Loc Relay opens",
                prefs.getBoolean(
                        GatewayPrefs.KEY_START_SERVICE_ON_APP_LAUNCH,
                        GatewayStartupPolicy.isStartServiceWithAppEnabledByDefault()),
                toggle -> startServiceWithAppSwitch = toggle,
                (button, checked) -> GatewayPrefs.get(this).edit()
                        .putBoolean(GatewayPrefs.KEY_START_SERVICE_ON_APP_LAUNCH, checked)
                        .apply()));
        content.addView(settingGuardedSwitchItem(
                R.drawable.ic_info_24,
                "Service notification",
                "Keep gateway in foreground",
                prefs.getBoolean(
                        GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED,
                        GatewayStartupPolicy.isPersistentNotificationEnabledByDefault()),
                toggle -> persistentNotificationSwitch = toggle,
                (toggle, checked) -> setPersistentNotificationEnabled(toggle, checked)));
        content.addView(settingSwitchItem(
                R.drawable.ic_no_window_24,
                "No window mode",
                "Hide from recent apps",
                prefs.getBoolean(GatewayPrefs.KEY_NO_WINDOW_MODE, false),
                null,
                (button, checked) -> {
                    GatewayPrefs.get(this).edit()
                            .putBoolean(GatewayPrefs.KEY_NO_WINDOW_MODE, checked)
                            .apply();
                    applyNoWindowModePreference();
                }));
        group.addView(content);
        root.addView(group, sectionParams());
        return scroll;
    }

    private void applyNoWindowModePreference() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return;
        }
        boolean exclude = GatewayPrefs.get(this).getBoolean(GatewayPrefs.KEY_NO_WINDOW_MODE, false);
        for (ActivityManager.AppTask task : manager.getAppTasks()) {
            task.setExcludeFromRecents(exclude);
        }
    }

    private void maybeStartGatewayWithApp() {
        SharedPreferences prefs = GatewayPrefs.get(this);
        boolean enabled = prefs.getBoolean(
                GatewayPrefs.KEY_START_SERVICE_ON_APP_LAUNCH,
                GatewayStartupPolicy.isStartServiceWithAppEnabledByDefault());
        if (GatewayStartupPolicy.shouldStartServiceWithApp(enabled, GatewayRuntimeState.isActive())) {
            startGateway(false);
        }
    }

    private void maybeRequestNotificationPermissionAfterManualStartIfReady() {
        SharedPreferences prefs = GatewayPrefs.get(this);
        boolean started = isGatewayStartedSuccessfully(prefs);
        boolean prompted = prefs.getBoolean(
                GatewayPrefs.KEY_SERVICE_NOTIFICATION_PERMISSION_PROMPTED_AFTER_START,
                false);
        if (!GatewayStartupPolicy.shouldRequestNotificationPermissionAfterManualStart(
                pendingManualStartNotificationPermissionCheck,
                started,
                prompted,
                NotificationPermissionPolicy.canPost(this))) {
            if (started) {
                pendingManualStartNotificationPermissionCheck = false;
            }
            refreshServiceNotificationWarning();
            return;
        }
        pendingManualStartNotificationPermissionCheck = false;
        prefs.edit()
                .putBoolean(GatewayPrefs.KEY_SERVICE_NOTIFICATION_PERMISSION_PROMPTED_AFTER_START, true)
                .apply();
        requestServiceNotificationPermission(PENDING_SETTING_PERMISSION_PERSISTENT_NOTIFICATION_AFTER_START);
    }

    private boolean explicitSettingPermissionReturn() {
        if (pendingSettingPermission == PENDING_SETTING_PERMISSION_NONE) {
            return false;
        }
        if (pendingSettingPermission == PENDING_SETTING_PERMISSION_AUTO_START) {
            boolean granted = AutoStartPermissionPolicy.isReady(this);
            GatewayPrefs.get(this).edit()
                    .putBoolean(
                            GatewayPrefs.KEY_AUTO_START_ENABLED,
                            GatewayStartupPolicy.nextGuardedSwitchState(true, granted))
                    .apply();
            pendingSettingPermission = PENDING_SETTING_PERMISSION_NONE;
            return !granted;
        }
        boolean granted = NotificationPermissionPolicy.canPost(this);
        GatewayPrefs.get(this).edit()
                .putBoolean(
                        GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED,
                        GatewayStartupPolicy.nextGuardedSwitchState(true, granted))
                .apply();
        pendingSettingPermission = PENDING_SETTING_PERMISSION_NONE;
        if (granted) {
            restartGatewayIfActive();
        }
        return !granted;
    }

    private void syncStartupSettingPermissions(boolean showDeniedMessage) {
        SharedPreferences prefs = GatewayPrefs.get(this);
        boolean autoStart = prefs.getBoolean(
                GatewayPrefs.KEY_AUTO_START_ENABLED,
                GatewayStartupPolicy.isAutoStartEnabledByDefault());
        boolean persistentNotification = prefs.getBoolean(
                GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED,
                GatewayStartupPolicy.isPersistentNotificationEnabledByDefault());
        boolean nextAutoStart = GatewayStartupPolicy.nextGuardedSwitchState(
                autoStart,
                AutoStartPermissionPolicy.isReady(this));
        boolean nextPersistentNotification = GatewayStartupPolicy.nextGuardedSwitchState(
                persistentNotification,
                NotificationPermissionPolicy.canPost(this));
        if (nextAutoStart != autoStart || nextPersistentNotification != persistentNotification) {
            prefs.edit()
                    .putBoolean(GatewayPrefs.KEY_AUTO_START_ENABLED, nextAutoStart)
                    .putBoolean(GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED, nextPersistentNotification)
                    .apply();
            if (nextPersistentNotification != persistentNotification) {
                restartGatewayIfActive();
            }
        }
        if (autoStartSwitch != null) {
            autoStartSwitch.setChecked(nextAutoStart);
        }
        if (startServiceWithAppSwitch != null) {
            startServiceWithAppSwitch.setChecked(prefs.getBoolean(
                    GatewayPrefs.KEY_START_SERVICE_ON_APP_LAUNCH,
                    GatewayStartupPolicy.isStartServiceWithAppEnabledByDefault()));
        }
        if (persistentNotificationSwitch != null) {
            persistentNotificationSwitch.setChecked(nextPersistentNotification);
        }
        if (showDeniedMessage) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void setAutoStartEnabled(SwitchMaterial toggle, boolean enabled) {
        if (!enabled) {
            GatewayPrefs.get(this).edit()
                    .putBoolean(GatewayPrefs.KEY_AUTO_START_ENABLED, false)
                    .apply();
            toggle.setChecked(false);
            return;
        }
        if (GatewayStartupPolicy.shouldRequestPermission(true, AutoStartPermissionPolicy.isReady(this))) {
            GatewayPrefs.get(this).edit()
                    .putBoolean(GatewayPrefs.KEY_AUTO_START_ENABLED, false)
                    .apply();
            toggle.setChecked(false);
            pendingSettingPermission = PENDING_SETTING_PERMISSION_AUTO_START;
            startActivity(AutoStartPermissionPolicy.guideIntent(this));
            Toast.makeText(this, "Allow unrestricted battery usage for auto start", Toast.LENGTH_SHORT).show();
            return;
        }
        GatewayPrefs.get(this).edit()
                .putBoolean(GatewayPrefs.KEY_AUTO_START_ENABLED, true)
                .apply();
        toggle.setChecked(true);
    }

    private void setPersistentNotificationEnabled(SwitchMaterial toggle, boolean enabled) {
        if (!enabled) {
            GatewayPrefs.get(this).edit()
                    .putBoolean(GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED, false)
                    .apply();
            toggle.setChecked(false);
            restartGatewayIfActive();
            return;
        }
        if (GatewayStartupPolicy.shouldRequestPermission(true, NotificationPermissionPolicy.canPost(this))) {
            GatewayPrefs.get(this).edit()
                    .putBoolean(GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED, false)
                    .apply();
            toggle.setChecked(false);
            requestPersistentNotificationPermissionForEnable();
            return;
        }
        GatewayPrefs.get(this).edit()
                .putBoolean(GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED, true)
                .apply();
        toggle.setChecked(true);
        restartGatewayIfActive();
    }

    private void requestPersistentNotificationPermissionForEnable() {
        requestServiceNotificationPermission(PENDING_SETTING_PERMISSION_PERSISTENT_NOTIFICATION);
    }

    private void requestServiceNotificationPermission(int pendingReason) {
        pendingSettingPermission = pendingReason;
        if (NotificationPermissionPolicy.requiresRuntimePermission()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_PERSISTENT_NOTIFICATION_PERMISSION);
            return;
        }
        openNotificationSettings();
        Toast.makeText(this, "Enable notifications for Loc Relay", Toast.LENGTH_SHORT).show();
    }

    private void requestServiceNotificationFromWarning() {
        if (NotificationPermissionPolicy.canPost(this)) {
            GatewayPrefs.get(this).edit()
                    .putBoolean(GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED, true)
                    .apply();
            restartGatewayIfActive();
            refreshServiceNotificationWarning();
            return;
        }
        requestPersistentNotificationPermissionForEnable();
    }

    private void handlePersistentNotificationPermissionResult(boolean granted) {
        if (pendingSettingPermission == PENDING_SETTING_PERMISSION_PERSISTENT_NOTIFICATION_AFTER_START) {
            pendingSettingPermission = PENDING_SETTING_PERMISSION_NONE;
            GatewayPrefs.get(this).edit()
                    .putBoolean(
                            GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED,
                            GatewayStartupPolicy.nextGuardedSwitchState(true, granted))
                    .apply();
            if (granted) {
                restartGatewayIfActive();
            }
            syncStartupSettingPermissions(false);
            return;
        }
        if (pendingSettingPermission != PENDING_SETTING_PERMISSION_PERSISTENT_NOTIFICATION) {
            return;
        }
        if (granted) {
            GatewayPrefs.get(this).edit()
                    .putBoolean(GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED, true)
                    .apply();
            pendingSettingPermission = PENDING_SETTING_PERMISSION_NONE;
            syncStartupSettingPermissions(false);
            restartGatewayIfActive();
            return;
        }
        GatewayPrefs.get(this).edit()
                .putBoolean(GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED, false)
                .apply();
        syncStartupSettingPermissions(false);
        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        openNotificationSettings();
    }

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= 26) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    private void restartGatewayIfActive() {
        if (GatewayRuntimeState.isActive()) {
            startGateway(false);
        }
    }

    private void showLogPage(String logKey, String title) {
        saveGatewayFormIfPresent(false);
        logReturnMenuId = currentMenuId;
        logReturnExtensionId = selectedExtensionId;
        selectedLogKey = logKey;
        clearPageReferences();
        updateBottomNavigationVisibility();
        configureToolbar(title, view -> restoreLogReturnPage(), null, null, view -> clearLog(logKey));
        replaceContent(buildLogPage(logKey));
        refreshLogText();
    }

    private void clearLog(String logKey) {
        if (!GatewayLogStore.isLogKey(logKey)) {
            return;
        }
        GatewayPrefs.get(this).edit().putString(logKey, "").apply();
        refreshLogText();
    }

    private void restoreLogReturnPage() {
        int targetMenuId = logReturnMenuId;
        String targetExtensionId = logReturnExtensionId;
        selectedLogKey = null;
        selectedExtensionId = targetExtensionId;
        showPage(targetMenuId);
    }

    private View buildLogPage(String logKey) {
        ScrollView scroll = pageScroll();
        logScrollView = scroll;
        LinearLayout root = pageRoot();
        scroll.addView(root);

        logTextView = text("", 13, COLOR_TEXT, false);
        logTextView.setTypeface(Typeface.MONOSPACE);
        logTextView.setTextIsSelectable(true);
        logTextView.setSingleLine(false);
        logTextView.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);

        MaterialCardView card = card(COLOR_SURFACE);
        LinearLayout content = cardContent();
        content.addView(logTextView);
        card.addView(content);
        root.addView(card, sectionParams());
        return scroll;
    }

    private void toggleGateway() {
        if (GatewayRuntimeState.isActive()) {
            stopGateway();
        } else {
            startGateway(true);
        }
    }

    private void startGateway(boolean manualStart) {
        startGateway(manualStart, true);
    }

    private void startGateway(boolean manualStart, boolean saveForm) {
        if (saveForm) {
            saveGatewayFormIfPresent(false);
        }
        pendingManualStartNotificationPermissionCheck = manualStart;
        GatewayPrefs.get(this).edit()
                .putBoolean(GatewayPrefs.KEY_RUNNING, true)
                .putString(GatewayPrefs.KEY_STATUS, "starting")
                .apply();
        GatewayRuntimeState.setActive(true);
        Intent intent = new Intent(this, GatewayService.class).setAction(GatewayService.ACTION_START);
        if (Build.VERSION.SDK_INT >= 26 && shouldStartGatewayInForeground(false)) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        refreshStatus();
    }

    private void stopGateway() {
        pendingManualStartNotificationPermissionCheck = false;
        GatewayPrefs.get(this).edit()
                .putBoolean(GatewayPrefs.KEY_RUNNING, false)
                .putString(GatewayPrefs.KEY_STATUS, "stopping")
                .apply();
        GatewayRuntimeState.setActive(false);
        Intent intent = new Intent(this, GatewayService.class).setAction(GatewayService.ACTION_STOP);
        startService(intent);
        refreshStatus();
        refreshStatusSoon();
    }

    private boolean shouldStartGatewayInForeground(boolean force) {
        if (force) {
            return true;
        }
        SharedPreferences prefs = GatewayPrefs.get(this);
        return prefs.getBoolean(
                GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED,
                GatewayStartupPolicy.isPersistentNotificationEnabledByDefault())
                && NotificationPermissionPolicy.canPost(this);
    }

    private boolean saveGatewayFormIfPresent(boolean feedback) {
        if (selectedExtensionId == null || !isGatewayEditable()) {
            return false;
        }
        GatewayExtension extension = GatewayExtensionCatalog.findById(selectedExtensionId);
        SharedPreferences.Editor editor = GatewayPrefs.get(this).edit();
        for (GatewayExtension.Field field : extension.getFields()) {
            editor.putString(field.getPrefKey(), valueOf(extensionInputs.get(field.getPrefKey())));
        }
        if (isBuiltInProxy(extension.getId()) && frpcRemotePortInput != null) {
            editor.putString(remotePortKeyFor(extension.getId()), valueOf(frpcRemotePortInput));
        }
        editor.apply();
        if (isBuiltInProxy(extension.getId())) {
            syncBuiltInFrpcProxy(extension.getId(), GatewayPrefs.get(this));
        }
        if (feedback) {
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void saveGatewayFormAndReturn() {
        if (saveGatewayFormIfPresent(true)) {
            selectedExtensionId = null;
            showPage(R.id.menu_extends);
        }
    }

    private void syncBuiltInFrpcProxy(String extensionId, SharedPreferences prefs) {
        if (!"http".equals(extensionId) && !"socks5".equals(extensionId)) {
            return;
        }
        if (frpcPublishSwitch == null || !frpcPublishSwitch.isChecked()) {
            FrpcProxyStore.remove(prefs, extensionId);
            return;
        }
        String bindHost = GatewayPrefs.getString(prefs, GatewayPrefs.KEY_BIND_HOST, GatewayPrefs.DEFAULT_BIND_HOST);
        if ("http".equals(extensionId)) {
            FrpcProxyStore.upsert(prefs, new ProxyMapping(
                    "http",
                    "tcp",
                    FrpcProcess.frpcLocalIp(bindHost),
                    GatewayPrefs.getInt(prefs, GatewayPrefs.KEY_HTTP_PORT, GatewayPrefs.DEFAULT_HTTP_PORT),
                    GatewayPrefs.getInt(prefs, GatewayPrefs.KEY_HTTP_REMOTE_PORT, GatewayPrefs.DEFAULT_HTTP_REMOTE_PORT)));
            return;
        }
        FrpcProxyStore.upsert(prefs, new ProxyMapping(
                "socks5",
                "tcp",
                FrpcProcess.frpcLocalIp(bindHost),
                GatewayPrefs.getInt(prefs, GatewayPrefs.KEY_SOCKS_PORT, GatewayPrefs.DEFAULT_SOCKS_PORT),
                GatewayPrefs.getInt(prefs, GatewayPrefs.KEY_SOCKS_REMOTE_PORT, GatewayPrefs.DEFAULT_SOCKS_REMOTE_PORT)));
    }

    private boolean isBuiltInProxy(String extensionId) {
        return "http".equals(extensionId) || "socks5".equals(extensionId);
    }

    private String remotePortKeyFor(String extensionId) {
        return "http".equals(extensionId)
                ? GatewayPrefs.KEY_HTTP_REMOTE_PORT
                : GatewayPrefs.KEY_SOCKS_REMOTE_PORT;
    }

    private int defaultRemotePortFor(String extensionId) {
        return "http".equals(extensionId)
                ? GatewayPrefs.DEFAULT_HTTP_REMOTE_PORT
                : GatewayPrefs.DEFAULT_SOCKS_REMOTE_PORT;
    }

    private String remotePortValueFor(String extensionId, SharedPreferences prefs) {
        ProxyMapping mapping = FrpcProxyStore.find(prefs, extensionId);
        if (mapping != null) {
            return Integer.toString(mapping.getRemotePort());
        }
        return GatewayPrefs.getString(
                prefs,
                remotePortKeyFor(extensionId),
                Integer.toString(defaultRemotePortFor(extensionId)));
    }

    private List<ProxyMapping> safeParseMappings(String value) {
        try {
            return FrpcProxyStore.parse(value);
        } catch (IllegalArgumentException ignored) {
            return new ArrayList<>();
        }
    }

    private int parsePortOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException err) {
            return defaultValue;
        }
    }

    private void refreshStatus() {
        boolean running = GatewayRuntimeState.isActive();
        if (statusText != null) {
            statusText.setText(running ? "Running" : "Stopped");
        }
        if (homeSummaryText != null) {
            homeSummaryText.setText(homeSummary());
        }
        if (controlButton != null) {
            controlButton.setText(running ? "STOP" : "START");
            styleControlButton(controlButton, running);
        }
        refreshServiceNotificationWarning();
        setExtensionEditable(!running);
    }

    private void refreshServiceNotificationWarning() {
        if (serviceNotificationWarning == null) {
            return;
        }
        SharedPreferences prefs = GatewayPrefs.get(this);
        boolean enabled = prefs.getBoolean(
                GatewayPrefs.KEY_PERSISTENT_NOTIFICATION_ENABLED,
                GatewayStartupPolicy.isPersistentNotificationEnabledByDefault());
        boolean show = GatewayStartupPolicy.shouldShowServiceNotificationWarning(
                isGatewayStartedSuccessfully(prefs),
                enabled,
                NotificationPermissionPolicy.canPost(this));
        serviceNotificationWarning.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private boolean isGatewayStartedSuccessfully(SharedPreferences prefs) {
        return prefs.getBoolean(GatewayPrefs.KEY_RUNNING, false)
                && "is running".equals(prefs.getString(GatewayPrefs.KEY_STATUS, ""));
    }

    private void refreshLogText() {
        if (logTextView == null || selectedLogKey == null) {
            return;
        }
        String log = GatewayLogStore.get(GatewayPrefs.get(this), selectedLogKey);
        logTextView.setText(log == null || log.trim().isEmpty() ? emptyLogText(selectedLogKey) : log);
        scheduleLogScrollToBottom();
    }

    private String emptyLogText(String logKey) {
        return GatewayLogStore.KEY_EVENT_LISTENER_LOG.equals(logKey) ? "No events yet" : "No logs yet";
    }

    private void scheduleLogScrollToBottom() {
        if (logScrollView == null) {
            return;
        }
        ScrollView scroll = logScrollView;
        scroll.post(() -> scroll.post(() -> {
            if (scroll != logScrollView) {
                return;
            }
            View content = scroll.getChildAt(0);
            int contentHeight = content == null ? 0 : content.getBottom();
            scroll.scrollTo(0, Math.max(0, contentHeight - scroll.getHeight()));
        }));
    }

    private String homeSummary() {
        SharedPreferences prefs = GatewayPrefs.get(this);
        int enabled = 0;
        if (prefs.getBoolean(GatewayPrefs.KEY_HTTP_ENABLED, true)) {
            enabled++;
        }
        if (prefs.getBoolean(GatewayPrefs.KEY_SOCKS_ENABLED, true)) {
            enabled++;
        }
        if (prefs.getBoolean(GatewayPrefs.KEY_FRPC_ENABLED, false)) {
            enabled++;
        }
        if (prefs.getBoolean(GatewayPrefs.KEY_EVENT_LISTENER_ENABLED, false)) {
            enabled++;
        }
        String status = prefs.getString(GatewayPrefs.KEY_STATUS, "");
        if (GatewayRuntimeState.isActive() && status != null && !status.trim().isEmpty()) {
            return enabled + " services enabled";
        }
        return enabled + " services ready";
    }

    private void refreshStatusSoon() {
        uiHandler.postDelayed(this::refreshStatus, 500);
        uiHandler.postDelayed(this::refreshStatus, 1500);
    }

    private void refreshNetworkInfo() {
        if (networkList != null) {
            renderNetworkInfo(NetworkInfoProvider.collect());
        }
    }

    private void renderNetworkInfo(List<NetworkInterfaceInfo> interfaces) {
        networkList.removeAllViews();
        int upCount = 0;
        int addressCount = 0;
        for (NetworkInterfaceInfo item : interfaces) {
            if (item.isUp()) {
                upCount++;
            }
            addressCount += item.getAddresses().size();
        }
        networkSummary.setText(interfaces.size() + " interfaces / " + upCount + " active / " + addressCount + " addresses");
        if (interfaces.isEmpty()) {
            MaterialCardView empty = card(COLOR_SURFACE);
            LinearLayout content = cardContent();
            content.addView(text("No network interfaces found", 16, COLOR_MUTED, false));
            empty.addView(content);
            networkList.addView(empty, sectionParams());
            return;
        }
        for (NetworkInterfaceInfo item : interfaces) {
            networkList.addView(networkInterfaceCard(item), sectionParams());
        }
    }

    private MaterialCardView networkInterfaceCard(NetworkInterfaceInfo item) {
        MaterialCardView card = card(COLOR_SURFACE);
        LinearLayout content = cardContent();

        TextView title = text(displayName(item), 18, COLOR_TEXT, true);
        content.addView(title);
        if (!item.getName().equals(displayName(item))) {
            TextView subtitle = text(item.getName(), 13, COLOR_MUTED, false);
            subtitle.setPadding(0, dp(2), 0, 0);
            content.addView(subtitle);
        }

        ChipGroup stateGroup = chipGroup();
        stateGroup.addView(chip(item.isUp() ? "Active" : "Down", item.isUp() ? COLOR_PRIMARY : COLOR_MUTED,
                item.isUp() ? COLOR_PRIMARY_CONTAINER : COLOR_SURFACE_VARIANT));
        stateGroup.addView(chip(item.isLoopback() ? "Loopback" : "Network", COLOR_MUTED, COLOR_SURFACE_VARIANT));
        stateGroup.addView(chip("MTU " + item.getMtu(), COLOR_MUTED, COLOR_SURFACE_VARIANT));
        content.addView(stateGroup, topMarginParams(dp(10)));

        if (item.getAddresses().isEmpty()) {
            TextView empty = text("No addresses", 14, COLOR_MUTED, false);
            empty.setPadding(0, dp(12), 0, 0);
            content.addView(empty);
        } else {
            TextView addressTitle = text("Addresses", 14, COLOR_MUTED, true);
            addressTitle.setPadding(0, dp(14), 0, dp(4));
            content.addView(addressTitle);
            for (String address : item.getAddresses()) {
                content.addView(addressRow(address), topMarginParams(dp(8)));
            }
        }

        card.addView(content);
        return card;
    }

    private LinearLayout addressRow(String address) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackgroundColor(COLOR_SURFACE_VARIANT);

        TextView kind = text(addressKind(address), 12, COLOR_MUTED, true);
        row.addView(kind);

        TextView value = text(address, 14, COLOR_TEXT, false);
        value.setTypeface(Typeface.MONOSPACE);
        value.setTextIsSelectable(true);
        value.setSingleLine(false);
        value.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
        value.setPadding(0, dp(4), 0, 0);
        row.addView(value);
        return row;
    }

    private void setExtensionEditable(boolean editable) {
        if (extensionEnabledSwitch != null) {
            extensionEnabledSwitch.setEnabled(editable);
        }
        for (TextInputEditText input : extensionInputs.values()) {
            input.setEnabled(editable);
        }
        if (frpcPublishSwitch != null) {
            frpcPublishSwitch.setEnabled(editable);
            updateFrpcRemotePortEnabled();
        }
        if (serverScanButton != null) {
            serverScanButton.setEnabled(editable);
            styleTonalIconButton(serverScanButton, editable);
        }
        if (addMappingButton != null) {
            addMappingButton.setEnabled(editable);
            styleTonalIconButton(addMappingButton, editable);
        }
        if (addWebhookButton != null) {
            addWebhookButton.setEnabled(editable);
            styleTonalIconButton(addWebhookButton, editable);
        }
        if (saveExtensionButton != null) {
            saveExtensionButton.setEnabled(editable);
            stylePrimaryIconButton(saveExtensionButton, editable);
        }
        for (CheckBox checkbox : eventChannelCheckboxes) {
            checkbox.setEnabled(editable);
        }
        setEventSwitchesEditable(editable);
    }

    private void setEventSwitchesEditable(boolean editable) {
        if (eventCallPermissionSwitch != null) {
            eventCallPermissionSwitch.setEnabled(editable);
        }
        if (eventSmsPermissionSwitch != null) {
            eventSmsPermissionSwitch.setEnabled(editable);
        }
        if (eventNotificationPermissionSwitch != null) {
            eventNotificationPermissionSwitch.setEnabled(editable);
        }
    }

    private void updateFrpcRemotePortEnabled() {
        if (frpcRemotePortInput != null) {
            frpcRemotePortInput.setEnabled(frpcPublishSwitch != null
                    && frpcPublishSwitch.isEnabled()
                    && frpcPublishSwitch.isChecked());
        }
    }

    private void clearPageReferences() {
        statusText = null;
        homeSummaryText = null;
        controlButton = null;
        serviceNotificationWarning = null;
        logTextView = null;
        logScrollView = null;
        networkSummary = null;
        networkList = null;
        extensionEnabledSwitch = null;
        frpcPublishSwitch = null;
        frpcRemotePortInput = null;
        frpcMappingList = null;
        webhookChannelList = null;
        serverScanButton = null;
        addMappingButton = null;
        addWebhookButton = null;
        saveExtensionButton = null;
        eventCallPermissionSwitch = null;
        eventSmsPermissionSwitch = null;
        eventNotificationPermissionSwitch = null;
        autoStartSwitch = null;
        startServiceWithAppSwitch = null;
        persistentNotificationSwitch = null;
        eventChannelCheckboxes.clear();
        extensionInputs.clear();
    }

    private GatewayExtensionSummary extensionSummary(GatewayExtension extension, SharedPreferences prefs) {
        boolean enabled = prefs.getBoolean(extension.getEnabledPrefKey(), extension.isEnabledByDefault());
        String endpointSummary;
        if ("http".equals(extension.getId())) {
            endpointSummary = GatewayPrefs.getString(prefs, GatewayPrefs.KEY_BIND_HOST, GatewayPrefs.DEFAULT_BIND_HOST)
                    + ":" + GatewayPrefs.getString(prefs, GatewayPrefs.KEY_HTTP_PORT,
                            Integer.toString(GatewayPrefs.DEFAULT_HTTP_PORT))
                    + frpcMappingSummary(prefs, "http")
                    + authSummary(prefs, GatewayPrefs.KEY_HTTP_AUTH_USERNAME);
            return new GatewayExtensionSummary(endpointSummary, enabled);
        }
        if ("socks5".equals(extension.getId())) {
            endpointSummary = GatewayPrefs.getString(prefs, GatewayPrefs.KEY_BIND_HOST, GatewayPrefs.DEFAULT_BIND_HOST)
                    + ":" + GatewayPrefs.getString(prefs, GatewayPrefs.KEY_SOCKS_PORT,
                            Integer.toString(GatewayPrefs.DEFAULT_SOCKS_PORT))
                    + frpcMappingSummary(prefs, "socks5")
                    + authSummary(prefs, GatewayPrefs.KEY_SOCKS_AUTH_USERNAME);
            return new GatewayExtensionSummary(endpointSummary, enabled);
        }
        String server = GatewayPrefs.getString(prefs, GatewayPrefs.KEY_FRPC_SERVER, "");
        String port = GatewayPrefs.getString(
                prefs,
                GatewayPrefs.KEY_FRPC_SERVER_PORT,
                Integer.toString(GatewayPrefs.DEFAULT_FRPC_SERVER_PORT));
        if ("event_listener".equals(extension.getId())) {
            EventListenerPermissionSnapshot snapshot = EventListenerPermissions.snapshot(this);
            endpointSummary = "permissions " + snapshot.grantedCount() + "/3";
            return new GatewayExtensionSummary(endpointSummary, enabled);
        }
        if ("webhook".equals(extension.getId())) {
            int count = WebhookChannelStore.parse(GatewayPrefs.getString(
                    prefs,
                    GatewayPrefs.KEY_WEBHOOK_CHANNELS,
                    "")).size();
            endpointSummary = count == 0 ? "no channels" : count + " channels";
            return new GatewayExtensionSummary(endpointSummary, enabled);
        }
        endpointSummary = server.isEmpty() ? "not configured" : server + ":" + port;
        return new GatewayExtensionSummary(endpointSummary, enabled);
    }

    private String authSummary(SharedPreferences prefs, String usernameKey) {
        return GatewayPrefs.getString(prefs, usernameKey, "").trim().isEmpty() ? "" : " / auth";
    }

    private String frpcMappingSummary(SharedPreferences prefs, String mappingName) {
        ProxyMapping mapping = FrpcProxyStore.find(prefs, mappingName);
        if (!prefs.getBoolean(GatewayPrefs.KEY_FRPC_ENABLED, false) || mapping == null) {
            return " / FRPC off";
        }
        return " -> " + mapping.getRemotePort();
    }

    private int inputType(GatewayExtension.FieldType type) {
        if (type == GatewayExtension.FieldType.NUMBER) {
            return InputType.TYPE_CLASS_NUMBER;
        }
        if (type == GatewayExtension.FieldType.SECRET) {
            return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        }
        return InputType.TYPE_CLASS_TEXT;
    }

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(COLOR_BACKGROUND);
        return scroll;
    }

    private LinearLayout pageRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        return root;
    }

    private MaterialCardView card(int color) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(color);
        card.setRadius(dp(8));
        card.setCardElevation(0);
        card.setStrokeColor(color == COLOR_SURFACE ? 0x00000000 : color);
        card.setStrokeWidth(0);
        return card;
    }

    private GradientDrawable sectionBackground(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), COLOR_OUTLINE);
        return background;
    }

    private LinearLayout cardContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));
        return content;
    }

    private LinearLayout compactCardContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(12));
        return content;
    }

    private View settingItem(int iconRes, String title, String subtitle, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(14), dp(14));
        row.setClickable(true);
        row.setOnClickListener(onClick);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(COLOR_PRIMARY);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        iconParams.setMargins(0, 0, dp(16), 0);
        row.addView(icon, iconParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title, 16, COLOR_TEXT, true));
        TextView subtitleView = text(subtitle, 13, COLOR_MUTED, false);
        subtitleView.setPadding(0, dp(3), 0, 0);
        copy.addView(subtitleView);
        row.addView(copy, weightWrapParams());

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron_right_24);
        chevron.setColorFilter(COLOR_MUTED);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(24)));
        return row;
    }

    private View settingSwitchItem(
            int iconRes,
            String title,
            String subtitle,
            boolean checked,
            SettingSwitchBinder binder,
            android.widget.CompoundButton.OnCheckedChangeListener onCheckedChange) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(14), dp(14));
        row.setClickable(true);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(COLOR_PRIMARY);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        iconParams.setMargins(0, 0, dp(16), 0);
        row.addView(icon, iconParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title, 16, COLOR_TEXT, true));
        TextView subtitleView = text(subtitle, 13, COLOR_MUTED, false);
        subtitleView.setPadding(0, dp(3), 0, 0);
        copy.addView(subtitleView);
        row.addView(copy, weightWrapParams());

        SwitchMaterial toggle = serviceSwitch(checked);
        toggle.setContentDescription(title);
        if (binder != null) {
            binder.bind(toggle);
        }
        toggle.setOnCheckedChangeListener(onCheckedChange);
        row.setOnClickListener(view -> toggle.setChecked(!toggle.isChecked()));
        row.addView(toggle, wrapParams());
        return row;
    }

    private View settingGuardedSwitchItem(
            int iconRes,
            String title,
            String subtitle,
            boolean checked,
            SettingSwitchBinder binder,
            SettingSwitchAction onToggle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(14), dp(14));
        row.setClickable(true);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(COLOR_PRIMARY);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        iconParams.setMargins(0, 0, dp(16), 0);
        row.addView(icon, iconParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title, 16, COLOR_TEXT, true));
        TextView subtitleView = text(subtitle, 13, COLOR_MUTED, false);
        subtitleView.setPadding(0, dp(3), 0, 0);
        copy.addView(subtitleView);
        row.addView(copy, weightWrapParams());

        SwitchMaterial toggle = serviceSwitch(checked);
        toggle.setContentDescription(title);
        if (binder != null) {
            binder.bind(toggle);
        }
        toggle.setOnClickListener(view -> onToggle.onToggle(toggle, toggle.isChecked()));
        row.setOnClickListener(view -> {
            boolean next = !toggle.isChecked();
            toggle.setChecked(next);
            onToggle.onToggle(toggle, next);
        });
        row.addView(toggle, wrapParams());
        return row;
    }

    private LinearLayout actionRow(View action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(action, wrapParams());
        return row;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(sizeSp);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private String displayName(NetworkInterfaceInfo item) {
        String displayName = item.getDisplayName();
        return displayName == null || displayName.trim().isEmpty() ? item.getName() : displayName;
    }

    private String addressKind(String address) {
        return address.contains(":") ? "IPv6" : "IPv4";
    }

    private ChipGroup chipGroup() {
        ChipGroup group = new ChipGroup(this);
        group.setSingleLine(false);
        group.setChipSpacingHorizontal(dp(6));
        group.setChipSpacingVertical(dp(4));
        return group;
    }

    private Chip chip(String label, int textColor, int backgroundColor) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setTextColor(textColor);
        chip.setTextSize(12);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
        chip.setChipStrokeColor(ColorStateList.valueOf(COLOR_OUTLINE));
        chip.setChipStrokeWidth(dp(1));
        chip.setEnsureMinTouchTargetSize(false);
        return chip;
    }

    private SwitchMaterial serviceSwitch(boolean checked) {
        SwitchMaterial switchView = new SwitchMaterial(this);
        switchView.setShowText(false);
        switchView.setTextOn("");
        switchView.setTextOff("");
        switchView.setMinWidth(dp(52));
        switchView.setMinimumWidth(dp(52));
        switchView.setThumbTintList(switchThumbColors());
        switchView.setTrackTintList(switchTrackColors());
        switchView.setChecked(checked);
        return switchView;
    }

    private ColorStateList switchThumbColors() {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked, android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{COLOR_PRIMARY, 0xffffffff, 0xffb8c2cc});
    }

    private ColorStateList switchTrackColors() {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked, android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_enabled},
                        new int[]{}
                },
                new int[]{0xff9bd9d2, 0xffccd7df, 0xffe1e7ec});
    }

    private ColorStateList navItemColors() {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{COLOR_PRIMARY, COLOR_MUTED});
    }

    private ColorStateList rippleColor() {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_pressed},
                        new int[]{android.R.attr.state_focused},
                        new int[]{}
                },
                new int[]{0x26006c67, 0x1c006c67, 0x14006c67});
    }

    private TextInputEditText addInput(LinearLayout parent, String label, GatewayExtension.FieldType fieldType) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(label);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxBackgroundColor(COLOR_SURFACE);
        layout.setBoxCornerRadii(dp(8), dp(8), dp(8), dp(8));
        layout.setBoxStrokeColor(COLOR_OUTLINE);
        layout.setHintTextColor(ColorStateList.valueOf(COLOR_MUTED));
        layout.setMinimumHeight(dp(64));
        TextInputEditText input = new TextInputEditText(layout.getContext());
        input.setTextColor(COLOR_TEXT);
        input.setInputType(inputType(fieldType));
        input.setIncludeFontPadding(false);
        input.setSingleLine(true);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setMinHeight(dp(56));
        input.setPadding(input.getPaddingLeft(), 0, input.getPaddingRight(), 0);
        layout.addView(input);
        parent.addView(layout, topMarginParams(dp(12)));
        return input;
    }

    private LinearLayout.LayoutParams sectionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        return params;
    }

    private LinearLayout.LayoutParams compactSectionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        return params;
    }

    private LinearLayout.LayoutParams topMarginParams(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, top, 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams bottomMarginParams(int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, bottom);
        return params;
    }

    private LinearLayout.LayoutParams fullWidthBottomMarginParams(int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, bottom);
        return params;
    }

    private LinearLayout.LayoutParams weightWrapParams() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout.LayoutParams wrapParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams squareParams(int size) {
        return new LinearLayout.LayoutParams(size, size);
    }

    private void styleControlButton(MaterialButton button, boolean running) {
        flattenButton(button);
        button.setCornerRadius(dp(72));
        button.setTextColor(running ? COLOR_ERROR_TEXT : Color.WHITE);
        button.setBackgroundTintList(ColorStateList.valueOf(running ? COLOR_ERROR_CONTAINER : COLOR_PRIMARY));
        button.setStrokeWidth(running ? dp(1) : 0);
        button.setStrokeColor(ColorStateList.valueOf(running ? 0xffffb4b4 : COLOR_PRIMARY));
    }

    private IconActionButton iconButton(int iconRes, String description, boolean primary) {
        return new IconActionButton(iconRes, description, INLINE_ICON_BUTTON_DP, ICON_DP, primary, false);
    }

    private IconActionButton toolbarIconButton(int iconRes, String description) {
        return new IconActionButton(iconRes, description, TOOLBAR_ICON_BUTTON_DP, ICON_DP, false, true);
    }

    private void stylePrimaryIconButton(IconActionButton button, boolean enabled) {
        button.setStyledEnabled(enabled);
    }

    private void styleTonalIconButton(IconActionButton button, boolean enabled) {
        button.setStyledEnabled(enabled);
    }

    private void flattenButton(MaterialButton button) {
        button.setStateListAnimator(null);
        button.setElevation(0f);
        button.setTranslationZ(0f);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setRippleColor(rippleColor());
    }

    private FrameLayout serviceIconBadge(int iconRes, String description) {
        FrameLayout badge = new FrameLayout(this);
        badge.setContentDescription(description);
        badge.setBackground(serviceIconBackground());

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(COLOR_PRIMARY);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        badge.addView(icon, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        return badge;
    }

    private GradientDrawable serviceIconBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(COLOR_PRIMARY_CONTAINER);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), 0xffb6dfd8);
        return background;
    }

    private final class IconActionButton extends FrameLayout {
        private final ImageView icon;
        private final boolean primary;
        private final boolean transparent;

        IconActionButton(int iconRes, String description, int buttonDp, int iconDp, boolean primary, boolean transparent) {
            super(MainActivity.this);
            this.primary = primary;
            this.transparent = transparent;
            setContentDescription(description);
            setClickable(true);
            setFocusable(true);
            setMinimumWidth(dp(buttonDp));
            setMinimumHeight(dp(buttonDp));

            icon = new ImageView(MainActivity.this);
            icon.setScaleType(ImageView.ScaleType.CENTER);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                    dp(iconDp),
                    dp(iconDp),
                    Gravity.CENTER);
            addView(icon, iconParams);
            setIconResource(iconRes);
            setStyledEnabled(true);
        }

        void setIconResource(int iconRes) {
            icon.setImageResource(iconRes);
        }

        void setStyledEnabled(boolean enabled) {
            setEnabled(enabled);
            setAlpha(enabled ? 1.0f : 0.55f);
            icon.setColorFilter(primary ? Color.WHITE : (enabled ? COLOR_PRIMARY : COLOR_MUTED));
            setBackground(iconBackground(primary, transparent, enabled));
        }
    }

    private GradientDrawable iconBackground(boolean primary, boolean transparent, boolean enabled) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(INLINE_ICON_BUTTON_DP / 2));
        if (transparent) {
            background.setColor(Color.TRANSPARENT);
            background.setStroke(0, Color.TRANSPARENT);
            return background;
        }
        if (primary) {
            background.setColor(enabled ? COLOR_PRIMARY : 0xffc5ccd4);
            background.setStroke(0, Color.TRANSPARENT);
        } else {
            background.setColor(enabled ? 0xffedf7f5 : 0xffeef1f4);
            background.setStroke(dp(1), enabled ? 0xffafd8d2 : COLOR_OUTLINE);
        }
        return background;
    }

    private String valueOf(TextInputEditText editText) {
        return editText == null || editText.getText() == null ? "" : editText.getText().toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
