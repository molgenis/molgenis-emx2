package org.molgenis.emx2;

import java.util.Map;
import java.util.Optional;

public interface HasSettingsInterface<T> {
  T clearSettings();

  T setSettings(Map<String, String> settings);

  T removeSetting(String key);

  T setSetting(String key, String value);

  String getSetting(String key);

  Map<String, String> getSettings();

  T changeSettings(Map<String, String> settings);

  Optional<String> findSettingValue(String cssURL);
}
