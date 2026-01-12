package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Row.row;

import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;

public class Emx2Changelog {

  private Emx2Changelog() {
    // Hide constructor
  }

  public static void outputChangelog(TableStore store, Schema schema) {
    if (!canAccessChangelog(schema)) {
      return;
    }

    List<Row> rows =
        schema.getChanges(schema.getChangesCount()).stream()
            .map(Emx2Changelog::changeToRow)
            .toList();

    store.writeTable(
        Constants.CHANGELOG_TABLE,
        List.of(
            Constants.CHANGELOG_OPERATION,
            Constants.CHANGELOG_STAMP,
            Constants.CHANGELOG_USERID,
            Constants.CHANGELOG_TABLENAME,
            Constants.CHANGELOG_OLD,
            Constants.CHANGELOG_NEW),
        rows);
  }

  private static Row changeToRow(Change change) {
    return row(
        Constants.CHANGELOG_OPERATION,
        change.operation(),
        Constants.CHANGELOG_STAMP,
        change.stamp(),
        Constants.CHANGELOG_USERID,
        change.userId(),
        Constants.CHANGELOG_TABLENAME,
        change.tableName(),
        Constants.CHANGELOG_OLD,
        change.oldRowData(),
        Constants.CHANGELOG_NEW,
        change.newRowData());
  }

  private static boolean canAccessChangelog(Schema schema) {
    var roles = schema.getInheritedRolesForActiveUser();
    return roles.contains(Privileges.MANAGER.toString())
        || roles.contains(Privileges.OWNER.toString());
  }
}
