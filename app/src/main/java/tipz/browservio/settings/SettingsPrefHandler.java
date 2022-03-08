package tipz.browservio.settings;

import static android.content.Context.ACTIVITY_SERVICE;
import static tipz.browservio.utils.urls.SearchEngineEntries.getHomepageUrl;
import static tipz.browservio.utils.urls.SearchEngineEntries.getSearchEngineUrl;
import static tipz.browservio.settings.SettingsUtils.browservio_saver;
import static tipz.browservio.utils.ApkInstaller.installApplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;

import tipz.browservio.BuildConfig;
import tipz.browservio.R;
import tipz.browservio.utils.urls.BrowservioURLs;
import tipz.browservio.utils.urls.SearchEngineEntries;
import tipz.browservio.utils.CommonUtils;

public class SettingsPrefHandler extends PreferenceFragmentCompat {
    public final Activity activity;

    public SettingsPrefHandler(Activity act) {
        this.activity = act;
    }

    private static boolean needReload = false;
    private long downloadID;
    private final String updateDownloadPath = Environment.getExternalStorageDirectory().getAbsolutePath()
            .concat("/").concat(Environment.DIRECTORY_DOWNLOADS).concat("/browservio-update.apk");

    final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadID == id) {
                installApplication(activity, updateDownloadPath);
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey);
        initializeLogic();
        activity.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activity.unregisterReceiver(onDownloadComplete);
    }

    public static boolean getNeedReload() {
        return needReload;
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void needLoad(String Url) {
        Intent needLoad = new Intent();
        needLoad.putExtra("needLoadUrl", Url);
        activity.setResult(0, needLoad);
        activity.finish();
    }

    /**
     * Initialize Logic
     */
    private void initializeLogic() {
        /* Common */
        final String[] searchHomePageList = {
                activity.getResources().getString(R.string.google_search),
                activity.getResources().getString(R.string.baidu_search),
                activity.getResources().getString(R.string.duck_search),
                activity.getResources().getString(R.string.bing_search),
                activity.getResources().getString(R.string.yahoo_search),
                activity.getResources().getString(R.string.ecosia_search),
                activity.getResources().getString(R.string.yandex_search),
                activity.getResources().getString(R.string.custom_search)
        };

        /* General category */
        Preference search_engine = Objects.requireNonNull(findPreference("search_engine"));
        Preference homepage = Objects.requireNonNull(findPreference("homepage"));
        CheckBoxPreference search_suggestions = Objects.requireNonNull(findPreference("search_suggestions"));

        /* Data & Privacy category */
        CheckBoxPreference adBlocker = Objects.requireNonNull(findPreference("adBlocker"));
        CheckBoxPreference do_not_track = Objects.requireNonNull(findPreference("do_not_track"));
        Preference reset_to_default = Objects.requireNonNull(findPreference("reset_to_default"));

        /* Visuals category */
        CheckBoxPreference show_favicon = Objects.requireNonNull(findPreference("show_favicon"));

        /* Advanced category */
        CheckBoxPreference javascript = Objects.requireNonNull(findPreference("javascript"));

        /* Help category */
        Preference version = Objects.requireNonNull(findPreference("version"));
        Preference feedback = Objects.requireNonNull(findPreference("feedback"));
        Preference source_code = Objects.requireNonNull(findPreference("source_code"));

        /* General category dialog */
        MaterialAlertDialogBuilder SearchSettingsDialog = new MaterialAlertDialogBuilder(activity);
        MaterialAlertDialogBuilder CustomSearchSettingsDialog = new MaterialAlertDialogBuilder(activity);
        MaterialAlertDialogBuilder HomepageSettingsDialog = new MaterialAlertDialogBuilder(activity);
        MaterialAlertDialogBuilder CustomHomepageSettingsDialog = new MaterialAlertDialogBuilder(activity);

        /* Data & Privacy dialog */
        MaterialAlertDialogBuilder ResetDialog = new MaterialAlertDialogBuilder(activity);

        /* Help category dialog */
        MaterialAlertDialogBuilder InfoDialog = new MaterialAlertDialogBuilder(activity);

        search_engine.setOnPreferenceClickListener(preference -> {
            final int[] checkedItem = {SettingsUtils.getPrefNum(browservio_saver(activity), SettingsKeys.defaultSearchId)};
            final String[] searchEngine = new String[1];
            SearchSettingsDialog.setTitle(getResources().getString(R.string.search_engine))
                    .setSingleChoiceItems(searchHomePageList,
                            SettingsUtils.getPrefNum(browservio_saver(activity), SettingsKeys.defaultSearchId), (dialog, which) -> checkedItem[0] = which)
                    .setPositiveButton(android.R.string.ok, (_dialog, _which) -> {
                        if (checkedItem[0] == 0)
                            searchEngine[0] = getSearchEngineUrl(SearchEngineEntries.google, SearchEngineEntries.googleSearchSuffix);
                        else if (checkedItem[0] == 1)
                            searchEngine[0] = getSearchEngineUrl(SearchEngineEntries.baidu, SearchEngineEntries.baiduSearchSuffix);
                        else if (checkedItem[0] == 2)
                            searchEngine[0] = getSearchEngineUrl(SearchEngineEntries.duck, SearchEngineEntries.duckSearchSuffix);
                        else if (checkedItem[0] == 3)
                            searchEngine[0] = getSearchEngineUrl(SearchEngineEntries.bing, SearchEngineEntries.bingSearchSuffix);
                        else if (checkedItem[0] == 4)
                            searchEngine[0] = getSearchEngineUrl(SearchEngineEntries.yahoo, SearchEngineEntries.yahooSearchSuffix);
                        else if (checkedItem[0] == 5)
                            searchEngine[0] = getSearchEngineUrl(SearchEngineEntries.ecosia, SearchEngineEntries.ecosiaSearchSuffix);
                        else if (checkedItem[0] == 6)
                            searchEngine[0] = getSearchEngineUrl(SearchEngineEntries.yandex, SearchEngineEntries.yandexSearchSuffix);
                        else if (checkedItem[0] == 7) {
                            final LayoutInflater layoutInflater = LayoutInflater.from(activity);
                            @SuppressLint("InflateParams") final View root = layoutInflater.inflate(R.layout.dialog_edittext, null);
                            final AppCompatEditText custom_se = root.findViewById(R.id.edittext);
                            CustomSearchSettingsDialog.setTitle(getResources().getString(R.string.search_engine))
                                    .setView(root)
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                        if (!Objects.requireNonNull(custom_se.getText()).toString().isEmpty()) {
                                            SettingsUtils.setPref(browservio_saver(activity), SettingsKeys.defaultSearch, custom_se.getText().toString());
                                            SettingsUtils.setPrefNum(browservio_saver(activity), SettingsKeys.defaultSearchId, checkedItem[0]);
                                            search_engine.setSummary(getResources().getString(R.string.search_engine_current, searchHomePageList[checkedItem[0]]));
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create().show();
                        }

                        if (checkedItem[0] != 7) {
                            SettingsUtils.setPref(browservio_saver(activity), SettingsKeys.defaultSearch, searchEngine[0]);
                            SettingsUtils.setPrefNum(browservio_saver(activity), SettingsKeys.defaultSearchId, checkedItem[0]);
                            search_engine.setSummary(getResources().getString(R.string.search_engine_current, searchHomePageList[checkedItem[0]]));
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
            return true;
        });

        homepage.setOnPreferenceClickListener(preference -> {
            final int[] checkedItem = {SettingsUtils.getPrefNum(browservio_saver(activity), SettingsKeys.defaultSearchId)};
            final String[] homePage = new String[1];
            HomepageSettingsDialog.setTitle(getResources().getString(R.string.homepage))
                    .setSingleChoiceItems(searchHomePageList,
                            SettingsUtils.getPrefNum(browservio_saver(activity), SettingsKeys.defaultHomePageId), (dialog, which) -> checkedItem[0] = which)
                    .setPositiveButton(android.R.string.ok, (_dialog, _which) -> {
                        if (checkedItem[0] == 0)
                            homePage[0] = getHomepageUrl(SearchEngineEntries.google);
                        else if (checkedItem[0] == 1)
                            homePage[0] = getHomepageUrl(SearchEngineEntries.baidu);
                        else if (checkedItem[0] == 2)
                            homePage[0] = getHomepageUrl(SearchEngineEntries.duck);
                        else if (checkedItem[0] == 3)
                            homePage[0] = getHomepageUrl(SearchEngineEntries.bing);
                        else if (checkedItem[0] == 4)
                            homePage[0] = getHomepageUrl(SearchEngineEntries.yahoo);
                        else if (checkedItem[0] == 5)
                            homePage[0] = getHomepageUrl(SearchEngineEntries.ecosia);
                        else if (checkedItem[0] == 6)
                            homePage[0] = getHomepageUrl(SearchEngineEntries.yandex);
                        else if (checkedItem[0] == 7) {
                            final LayoutInflater layoutInflater = LayoutInflater.from(activity);
                            @SuppressLint("InflateParams") final View root = layoutInflater.inflate(R.layout.dialog_edittext, null);
                            final AppCompatEditText custom_se = root.findViewById(R.id.edittext);
                            CustomHomepageSettingsDialog.setTitle(getResources().getString(R.string.homepage))
                                    .setView(root)
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                        if (!Objects.requireNonNull(custom_se.getText()).toString().isEmpty()) {
                                            SettingsUtils.setPref(browservio_saver(activity), SettingsKeys.defaultHomePage, custom_se.getText().toString());
                                            SettingsUtils.setPrefNum(browservio_saver(activity), SettingsKeys.defaultHomePageId, checkedItem[0]);
                                            homepage.setSummary(getResources().getString(R.string.homepage_current, searchHomePageList[checkedItem[0]]));
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create().show();
                        }

                        if (checkedItem[0] != 7) {
                            SettingsUtils.setPref(browservio_saver(activity), SettingsKeys.defaultHomePage, homePage[0]);
                            SettingsUtils.setPrefNum(browservio_saver(activity), SettingsKeys.defaultHomePageId, checkedItem[0]);
                            homepage.setSummary(getResources().getString(R.string.homepage_current, searchHomePageList[checkedItem[0]]));
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
            return true;
        });

        search_suggestions.setOnPreferenceClickListener(preference -> {
            SettingsUtils.setPrefIntBoolAccBool(browservio_saver(activity),
                    SettingsKeys.enableSuggestions, search_suggestions.isChecked(), false);
            return true;
        });

        adBlocker.setOnPreferenceClickListener(preference -> {
            SettingsUtils.setPrefIntBoolAccBool(browservio_saver(activity),
                    SettingsKeys.enableAdBlock, adBlocker.isChecked(), false);
            needReload = true;
            return true;
        });

        do_not_track.setOnPreferenceClickListener(preference -> {
            SettingsUtils.setPrefStringBoolAccBool(browservio_saver(activity),
                    SettingsKeys.sendDNT, do_not_track.isChecked(), false);
            needReload = true;
            return true;
        });

        reset_to_default.setOnPreferenceClickListener(preference -> {
            ResetDialog.setTitle(getResources().getString(R.string.reset_btn))
                    .setMessage(getResources().getString(R.string.reset_dialog).concat(getResources().getString(R.string.to_continue)))
                    .setPositiveButton(getResources().getString(R.string.clear, CommonUtils.EMPTY_STRING).trim(), (_dialog, _which) -> {
                        CommonUtils.showMessage(activity, getResources().getString(R.string.reset_complete));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            ((ActivityManager) activity.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                        } else {
                            String packageName = activity.getPackageName();
                            Runtime runtime = Runtime.getRuntime();
                            try {
                                runtime.exec("pm clear " + packageName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
            return true;
        });

        show_favicon.setOnPreferenceClickListener(preference -> {
            SettingsUtils.setPrefStringBoolAccBool(browservio_saver(activity),
                    SettingsKeys.showFavicon, show_favicon.isChecked(), false);
            return true;
        });

        javascript.setOnPreferenceClickListener(preference -> {
            SettingsUtils.setPrefStringBoolAccBool(browservio_saver(activity),
                    SettingsKeys.isJavaScriptEnabled, javascript.isChecked(), false);
            needReload = true;
            return true;
        });

        version.setOnPreferenceClickListener(preference -> {
            @SuppressLint("InflateParams") View dialogView = this.getLayoutInflater().inflate(R.layout.about_dialog, null);
            AppCompatImageView easter_banner = dialogView.findViewById(R.id.easter_banner);
            AppCompatTextView dialog_text = dialogView.findViewById(R.id.dialog_text);
            AppCompatButton update_btn = dialogView.findViewById(R.id.update_btn);
            AppCompatButton changelog_btn = dialogView.findViewById(R.id.changelog_btn);
            AppCompatButton license_btn = dialogView.findViewById(R.id.license_btn);
            if (BuildConfig.BUILD_TYPE.equals("debug") && SettingsUtils.getPrefNum(browservio_saver(activity), SettingsKeys.updateTesting) != 1) {
                update_btn.setVisibility(View.GONE);
                changelog_btn.setVisibility(View.GONE);
            }
            easter_banner.setOnClickListener(_update_btn -> {
                CommonUtils.showMessage(activity, String.format(Locale.ENGLISH, "%03d", 0).replace("0", getResources().getString(R.string.app_name).concat("! ")));
                SettingsUtils.setPrefNum(browservio_saver(activity), SettingsKeys.updateTesting, 1);
            });
            dialog_text.setText(getResources().getString(R.string.version_info_message,
                    getResources().getString(R.string.app_name),
                    BuildConfig.VERSION_NAME.concat(BuildConfig.VERSION_NAME_EXTRA),
                    String.valueOf(BuildConfig.VERSION_CODE).concat(".").concat(BuildConfig.BUILD_TYPE).concat(".").concat(BuildConfig.VERSION_BUILD_DATE),
                    BuildConfig.VERSION_CODENAME,
                    BuildConfig.VERSION_BUILD_YEAR));
            update_btn.setOnClickListener(_update_btn -> {
                if (!isNetworkAvailable(activity.getApplicationContext())) {
                    CommonUtils.showMessage(activity.getApplicationContext(), getResources().getString(R.string.network_unavailable_toast));
                } else {
                    new Thread() {
                        @Override
                        public void run() {
                            String path = "https://gitlab.com/TipzTeam/browservio/-/raw/update_files/api2.cfg";
                            File apkFile = new File(updateDownloadPath);
                            URL u;
                            try {
                                u = new URL(path);
                                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                                c.setRequestMethod("GET");
                                c.connect();
                                final ByteArrayOutputStream bo = new ByteArrayOutputStream();
                                byte[] buffer = new byte[1024];
                                int inputStreamTest = c.getInputStream().read(buffer);
                                if (inputStreamTest >= 5) {
                                    bo.write(buffer);

                                    activity.runOnUiThread(() -> {
                                        int position = 0;
                                        boolean isLatest = false;
                                        String[] array = bo.toString().split(CommonUtils.LINE_SEPARATOR());
                                        for (String obj : array) {
                                            if (position == 0) {
                                                if (Integer.parseInt(obj) <= BuildConfig.VERSION_CODE && SettingsUtils.getPrefNum(browservio_saver(activity), SettingsKeys.updateTesting) == 1) {
                                                    isLatest = true;
                                                    CommonUtils.showMessage(activity.getApplicationContext(), getResources().getString(R.string.version_latest_toast));
                                                }
                                            }
                                            if (position == 1 && !isLatest) {
                                                CommonUtils.showMessage(activity.getApplicationContext(), getResources().getString(R.string.new_update_detect_toast));

                                                if (!apkFile.exists() || apkFile.delete()) {
                                                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(obj));
                                                    request.setTitle(getResources().getString(R.string.download_title));
                                                    request.setMimeType("application/vnd.android.package-archive");
                                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "browservio-update.apk");
                                                    DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
                                                    downloadID = dm.enqueue(request);
                                                } else {
                                                    CommonUtils.showMessage(activity.getApplicationContext(), getResources().getString(R.string.update_down_failed_toast));
                                                }
                                            }
                                            position += 1;
                                        }
                                        try {
                                            bo.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                } else {
                                    CommonUtils.showMessage(activity.getApplicationContext(), getResources().getString(R.string.update_down_failed_toast));
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }.start();
                }
            });
            changelog_btn.setOnClickListener(_license_btn -> needLoad(BrowservioURLs.realChangelogUrl));
            license_btn.setOnClickListener(_license_btn -> needLoad(BrowservioURLs.licenseUrl));

            InfoDialog.setView(dialogView)
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
            return true;
        });

        feedback.setOnPreferenceClickListener(preference -> {
            needLoad(BrowservioURLs.feedbackUrl);
            return true;
        });

        source_code.setOnPreferenceClickListener(preference -> {
            needLoad(BrowservioURLs.sourceUrl);
            return true;
        });

        checkIfPrefIntIsTrue(SettingsKeys.enableAdBlock, adBlocker, true);
        checkIfPrefIntIsTrue(SettingsKeys.sendDNT, do_not_track, false);
        checkIfPrefIntIsTrue(SettingsKeys.enableSuggestions, search_suggestions, true);
        checkIfPrefIntIsTrue(SettingsKeys.showFavicon, show_favicon, false);
        checkIfPrefIntIsTrue(SettingsKeys.isJavaScriptEnabled, javascript, false);
        search_engine.setSummary(getResources().getString(R.string.search_engine_current, searchHomePageList[SettingsUtils.getPrefNum(browservio_saver(activity), SettingsKeys.defaultSearchId)]));
        homepage.setSummary(getResources().getString(R.string.homepage_current, searchHomePageList[SettingsUtils.getPrefNum(browservio_saver(activity), SettingsKeys.defaultHomePageId)]));
        version.setSummary(getResources().getString(R.string.app_name).concat(" ").concat(BuildConfig.VERSION_NAME.concat(BuildConfig.VERSION_NAME_EXTRA)));
        needReload = false;
    }

    private void checkIfPrefIntIsTrue(String tag, CheckBoxPreference checkBox, boolean isInt) {
        checkBox.setChecked(CommonUtils.isIntStrOne(isInt ? SettingsUtils.getPrefNum(browservio_saver(activity), tag) : SettingsUtils.getPref(browservio_saver(activity), tag)));
    }
}