package io.github.nvcex.android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (App.xposed() == null) {
            // debugビルドだと、xposed無しで設定できる
            // 設定画面の開発用機能
            setTitle(getTitle() + " (no LSPosed)");
        }
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        Log.i(this.getClass().toString(), "getSharedPreferences called: " + name);
        var xposed = App.xposed();
        if (xposed != null) {
            return xposed.getRemotePreferences(name);
        }
        return super.getSharedPreferences(name, mode);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private List<String> scenarioList;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            for (var key : List.of("voicevox_engine_url", "voicevox_engine_username", "voicevox_engine_password")) {
                var p = (EditTextPreference)findPreference(key);
                if (p != null) {
                    p.setOnBindEditTextListener(TextView::setSingleLine);
                }
            }

            final var button = findPreference("get_voicevox_voices");
            button.setOnPreferenceClickListener(p -> {
                button.setEnabled(false);
                App.executor.execute(() -> {
                    var prefs = getPreferenceManager().getSharedPreferences();
                    var api = new NvcexServerApi(
                            prefs.getString("voicevox_engine_url", null),
                            prefs.getString("voicevox_engine_username", null),
                            prefs.getString("voicevox_engine_password", null));
                    try {
                        var scenarios = api.scenarios();
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "NvcexServer api success", Toast.LENGTH_SHORT).show();
                            button.setEnabled(true);
                            updateScenarioList(scenarios);
                            var mapper = new ObjectMapper();
                            try {
                                getPreferenceManager().getSharedPreferences()
                                        .edit()
                                        .putString("scenarios", mapper.writeValueAsString(scenarios))
                                        .apply();
                            } catch (JsonProcessingException ignore) {
                            }
                        });
                    } catch (IOException ioe) {
                        Log.w(getClass().getName(), "scenarios() failed", ioe);
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "NvcexServer api failed " + ioe.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
                return true;
            });

            var scenarios = getPreferenceManager().getSharedPreferences().getString("scenarios", "[]");
            var mapper = new ObjectMapper();
            try {
                updateScenarioList(mapper.readValue(scenarios, new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException ignore) {
            }
        }

        private void updateScenarioList(final List<String> scenarios) {
            this.scenarioList = scenarios;
            final var prefPlayer = (ListPreference)findPreference("scenario");
            prefPlayer.setEntries(scenarios.toArray(String[]::new));
            prefPlayer.setEntryValues(scenarios.toArray(String[]::new));
        }

//        private boolean updateStyle(String playerId) {
//            final var prefStyle = (ListPreference)findPreference("style");
//            var player = this.playerList.stream().filter(p -> p.uuid.equals(playerId)).findFirst();
//            if (player.isPresent()) {
//                var styles = player.get().styles;
//                prefStyle.setEntries(styles.stream().map(s -> s.name).toArray(String[]::new));
//                var values = styles.stream().map(s -> Integer.toString(s.id)).toArray(String[]::new);
//                var defaultValue = values.length == 0 ? null : values[0];
//                prefStyle.setEntryValues(values);
//                var value = prefStyle.getValue();
//                prefStyle.setValue(Arrays.stream(values).filter(v -> v == value).findFirst().orElse(defaultValue));
//                return true;
//            } else {
//                return false;
//            }
//        }
    }
}
