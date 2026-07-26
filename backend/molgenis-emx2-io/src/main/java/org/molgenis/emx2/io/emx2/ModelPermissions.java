package org.molgenis.emx2.io.emx2;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;

/**
 * Applies bundle {@code permissions:} role defaults to a schema. Role names must match the exact,
 * case-sensitive names shown in the UI (see {@link Privileges}); an unknown role fails loudly and
 * lists the legal names. Membership is added idempotently, so re-applying a bundle is safe.
 */
public final class ModelPermissions {

  private ModelPermissions() {}

  public static void apply(Schema schema, Map<String, String> permissions) {
    for (Map.Entry<String, String> entry : permissions.entrySet()) {
      String role = entry.getKey();
      if (!Privileges.isSystemRole(role)) {
        throw new MolgenisException(
            "unknown permission role '"
                + role
                + "' in schema '"
                + schema.getName()
                + "'; legal roles are "
                + legalRoleNames());
      }
      schema.addMember(entry.getValue(), role);
    }
  }

  public static String legalRoleNames() {
    return Arrays.stream(Privileges.values())
        .map(Privileges::toString)
        .collect(Collectors.joining(", "));
  }
}
