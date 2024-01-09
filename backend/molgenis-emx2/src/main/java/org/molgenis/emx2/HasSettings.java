package org.molgenis.emx2;

import java.util.*;

public class HasSettings<T> implements HasSettingsInterface<T> {
  private Map<String, String> settings = new LinkedHashMap<>();

  @Override
  public T clearSettings() {
    this.settings.clear();
    return (T) this;
  }

  @Override
  public T removeSetting(String key) {
    // this is so that subclasses only need override setSettings(map)
    Map<String, String> settings = new LinkedHashMap<>();
    settings.putAll(getSettings());
    settings.remove(key);
    this.setSettings(settings);
    return (T) this;
  }

  @Override
  public T setSettings(Map<String, String> settings) {
    this.settings.clear();
    if (settings == null) return (T) this;
    this.settings.putAll(settings);
    return (T) this;
  }

  @Override
  public T setSetting(String key, String value) {
    Map<String, String> changedSettings = new LinkedHashMap<>();
    changedSettings.put(key, value);
    return changeSettings(changedSettings);
  }

  @Override
  public String getSetting(String key) {
    return this.settings.get(key);
  }

  @Override
  public Map<String, String> getSettings() {
    return Collections.unmodifiableMap(this.settings);
  }

  public void setSettingsWithoutReload(Map<String, String> settings) {
    this.settings.clear();
    if (settings != null) {
      this.settings.putAll(settings);
    }
  }

  @Override
  public T changeSettings(Map<String, String> changedSettings) {
    // this is so that subclasses only need override setSettings(map)
    Map<String, String> settings = new LinkedHashMap<>();
    settings.putAll(getSettings());
    settings.putAll(changedSettings);
    this.setSettings(settings);
    return (T) this;
  }

  @Override
  public Optional<String> findSettingValue(String key) {
    if (settings.containsKey(key) && settings.get(key) != null) {
      return Optional.of(settings.get(key));
    } else {
      return Optional.empty();
    }
  }
}
