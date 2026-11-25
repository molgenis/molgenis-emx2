package org.molgenis.emx2;

import java.util.Map;

public abstract class AbstractDatabase extends HasSettings<Database> implements Database {
  @Override
  public final Database setSettings(Map<String, String> settings) {
    if (!isAdmin()) {
      throw new MolgenisException("Insufficient rights to create database level setting");
    }
    DatabaseSetting.validate(settings);
    super.setSettings(settings);
    saveSettings(getSettings());
    return this;
  }

  /**
   * Method that should be implemented that contains the logic for actually storing the settings.
   */
  protected abstract void saveSettings(Map<String, String> settings);
}
