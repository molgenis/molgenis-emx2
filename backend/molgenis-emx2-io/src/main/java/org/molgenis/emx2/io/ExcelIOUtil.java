package org.molgenis.emx2.io;

import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.Row;

public class ExcelIOUtil {

  public static String getExportStringValue(Row row, String key) {
    String cellValue = row.getString(key);
    if (cellValue != null && cellValue.startsWith("=")) {
      return "'" + cellValue;
    } else {
      return cellValue;
    }
  }

  @NotNull
  public static String getImportStringValue(String rawValue) {
    String trimmed = rawValue.trim();
    if (trimmed.startsWith("'=")) {
      return trimmed.substring(1);
    } else {
      return trimmed;
    }
  }
}
