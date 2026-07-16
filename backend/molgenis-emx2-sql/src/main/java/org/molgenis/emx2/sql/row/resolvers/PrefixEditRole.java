package org.molgenis.emx2.sql.row.resolvers;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Row;

public class PrefixEditRole {

  private PrefixEditRole() {
    // hide constructor
  }

  public static void apply(Column column, Row row) {
    row.setString(
        column.getName(), Constants.MG_USER_PREFIX + row.getString(Constants.MG_EDIT_ROLE));
  }
}
