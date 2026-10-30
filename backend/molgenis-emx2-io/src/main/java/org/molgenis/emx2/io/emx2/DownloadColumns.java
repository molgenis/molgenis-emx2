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

  public static List<String> resolve(
      TableMetadata metadata, List<String> requested, boolean includeSystemColumns) {
    List<String> downloadNames =
        metadata.getDownloadColumnNames().stream().map(Column::getName).toList();
    if (requested == null || requested.isEmpty()) {
      return downloadNames.stream().filter(name -> isIncluded(name, includeSystemColumns)).toList();
    }

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

    List<String> unknownRequestedColumnIdentifiers = new ArrayList<>();
    Set<String> result = new LinkedHashSet<>();
    for (String nameOrId : requested) {
      if (nameOrId.contains(".")) {
        result.add(nameOrId);
        continue;
      }
      Column column = metadata.getColumn(nameOrId);
      if (column == null) {
        column = metadata.getColumnByIdentifier(nameOrId);
      }
      if (column == null || column.isHeading()) {
        unknownRequestedColumnIdentifiers.add(nameOrId);
      } else {
        expand(column).stream().filter(downloadNames::contains).forEach(result::add);
      }
    }
    if (!unknownRequestedColumnIdentifiers.isEmpty()) {
      throw new MolgenisException(
          "Cannot download unknown column(s) "
              + String.join(", ", unknownRequestedColumnIdentifiers)
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
