package org.molgenis.emx2;

/*
 * UpdateMode defines how the passed data should be applied to the existing table data.
 *
 * OVERWRITE: The existing data in the table will be completely replaced by the new data from the CSV.
 * UPDATE: The existing data in the table will be updated with the new data from the CSV, preserving any existing data that is not present in the CSV.
 */
public enum UpdateMode {
  OVERWRITE,
  UPDATE;

  public static final UpdateMode DEFAULT_MODE = OVERWRITE;
}
