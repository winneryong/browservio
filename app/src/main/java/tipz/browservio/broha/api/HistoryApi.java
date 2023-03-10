package tipz.browservio.broha.api;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import tipz.browservio.Application;
import tipz.browservio.broha.database.Broha;
import tipz.browservio.broha.database.BrohaDao;
import tipz.browservio.settings.SettingsKeys;
import tipz.browservio.settings.SettingsUtils;
import tipz.browservio.utils.CommonUtils;

public class HistoryApi {
    private final static int LATEST_API = 2;

    /* Old pref keys for migration */
    private static final String history = "history";

    private static SharedPreferences historyPref(Context context) {
        return context.getSharedPreferences("history.cfg", Activity.MODE_PRIVATE);
    }

    public static BrohaDao historyBroha(Context context) {
        return ((Application) context.getApplicationContext()).historyBroha;
    }

    public static void doApiInitCheck(Context context) {
        SharedPreferences pref = ((Application) context.getApplicationContext()).pref;

        if (SettingsUtils.getPrefNum(pref, SettingsKeys.historyApi) > LATEST_API
                || SettingsUtils.getPrefNum(pref, SettingsKeys.historyApi) <= -1)
            throw new RuntimeException();

        String historyData;
        switch (SettingsUtils.getPrefNum(pref, SettingsKeys.historyApi)) {
            case 0:
                historyData = SettingsUtils.getPref(pref, history);
                if (!historyData.isEmpty())
                    SettingsUtils.setPref(historyPref(context), history, historyData);

                SettingsUtils.setPref(pref, history, CommonUtils.EMPTY_STRING);
            case 1:
                historyData = SettingsUtils.getPref(historyPref(context), history);
                String[] listData = SettingsUtils.getPref(historyPref(context), history).trim().split("\n");
                if (!historyData.isEmpty())
                    for (String listDatum : listData)
                        historyBroha(context).insertAll(
                                new Broha(listDatum));
                historyPref(context).edit().clear().apply();
        }
        SettingsUtils.setPrefNum(pref, SettingsKeys.historyApi, LATEST_API);
    }
}
