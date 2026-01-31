package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.INCLUDE_SYSTEM_COLUMNS;

import io.javalin.http.Context;
import java.util.List;
import java.util.Optional;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlSchemaMetadata;

public class DownloadApiUtils {

  private DownloadApiUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  static boolean includeSystemColumns(Context ctx) {
    return String.valueOf(ctx.queryParam(INCLUDE_SYSTEM_COLUMNS)).equalsIgnoreCase("true");
  }

  static boolean isManagerOrOwnerOfSchema(Context ctx, Schema schema) {
    String currentUser = new MolgenisSessionHandler(ctx.req()).getCurrentUser();
    SqlSchemaMetadata sqlSchemaMetadata =
        new SqlSchemaMetadata(schema.getDatabase(), schema.getName());
    List<String> roles = sqlSchemaMetadata.getInheritedRolesForUser(currentUser);
    return roles.contains(Privileges.MANAGER.toString())
        || roles.contains(Privileges.OWNER.toString());
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
}
