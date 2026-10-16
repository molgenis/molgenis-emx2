package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Constants.MG_DRAFT;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.TableMetadata;

public class DownloadColumns {
  private DownloadColumns() {
    // hidden
  }

  public static boolean isIncluded(String columnName, boolean includeSystemColumns) {
    return columnName.equals(MG_DRAFT) || !columnName.startsWith("mg_") || includeSystemColumns;
  }

  /**
   * @param requested names or identifiers of columns of this table, including columns inherited
   *     from its parent tables, in the order wanted in the output; null or empty means all columns.
   *     A reference expands to the key column(s) of the row it refers to: one column, or one
   *     dot-separated column per key part when that key is composite (e.g. owner.firstName,
   *     owner.lastName). Such a key part can also be requested on its own; no other dot-separated
   *     name is allowed. A file expands to its file and filename columns. Explicitly requested
   *     system columns are always included.
   * @throws MolgenisException when a requested column is not a key part but dot-separated, does not
   *     exist or holds no data
   */
  public static List<String> resolve(
      TableMetadata metadata, List<String> requested, boolean includeSystemColumns) {
    List<String> downloadNames =
        metadata.getDownloadColumnNames().stream().map(Column::getName).toList();
    if (requested == null || requested.isEmpty()) {
      return downloadNames.stream().filter(name -> isIncluded(name, includeSystemColumns)).toList();
    }

    // the only dot-separated download columns are key parts of composite key references
    List<String> dotted =
        requested.stream()
            .filter(name -> name.contains(".") && !downloadNames.contains(name))
            .toList();
    if (!dotted.isEmpty()) {
      throw new MolgenisException(
          "Cannot download dot-separated column(s) "
              + String.join(", ", dotted)
              + "; only key parts of composite key references, such as owner.firstName, are"
              + " allowed");
    }

    List<String> unknown = new ArrayList<>();
    Set<String> result = new LinkedHashSet<>();
    for (String nameOrId : requested) {
      if (nameOrId.contains(".")) {
        result.add(nameOrId);
        continue;
      }
      // getColumn and getColumnByIdentifier also find columns inherited from parent tables
      Column column = metadata.getColumn(nameOrId);
      if (column == null) {
        column = metadata.getColumnByIdentifier(nameOrId);
      }
      if (column == null || column.isHeading()) {
        unknown.add(nameOrId);
      } else {
        // overlapping refLink keys also appear in the full list, via the column they belong to
        expand(column).stream().filter(downloadNames::contains).forEach(result::add);
      }
    }
    if (!unknown.isEmpty()) {
      throw new MolgenisException(
          "Cannot download unknown column(s) "
              + String.join(", ", unknown)
              + " of table "
              + metadata.getTableName());
    }
    return new ArrayList<>(result);
  }

  private static List<String> expand(Column column) {
    if (column.isFile()) {
      return List.of(column.getName(), column.getName() + "_filename");
    } else if (column.isReference()) {
      return column.getReferences().stream().map(Reference::getColumnName).toList();
    } else {
      return List.of(column.getName());
    }
  }
}
