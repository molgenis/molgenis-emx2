package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.INCLUDE_SYSTEM_COLUMNS;

import io.javalin.http.Context;
import java.util.List;
import java.util.Optional;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Table;

public class DownloadApiUtils {

  private DownloadApiUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  static boolean includeSystemColumns(Context ctx) {
    return String.valueOf(ctx.queryParam(INCLUDE_SYSTEM_COLUMNS)).equalsIgnoreCase("true");
  }

  static Optional<Integer> parseIntParam(Context ctx, String param) {
    return Optional.ofNullable(ctx.queryParam(param))
        .map(
            arg -> {
              try {
                return Integer.valueOf(arg);
              } catch (NumberFormatException e) {
                throw new MolgenisException(
                    "Invalid " + param + " provided, should be a number", e);
              }
            });
  }

  static String extractIdFromPath(Context ctx, Table table) {
    String tablePath = "/" + table.getName() + "/";
    int tableIndex = ctx.path().indexOf(tablePath);
    if (tableIndex == -1) {
      throw new MolgenisException("Invalid path for table " + table.getName());
    }
    return ctx.path().substring(tableIndex + tablePath.length());
  }

  static void validatePrimaryKeyCount(List<String> primaryKeyNames) {
    if (primaryKeyNames.isEmpty()) {
      throw new MolgenisException("Table has no primary key");
    }
  }

  static void validateCompositeKeyParts(String[] parts, List<String> primaryKeyNames) {
    if (parts.length != primaryKeyNames.size()) {
      throw new MolgenisException(
          "Composite primary key requires "
              + primaryKeyNames.size()
              + " values separated by /, got "
              + parts.length);
    }
  }
}
