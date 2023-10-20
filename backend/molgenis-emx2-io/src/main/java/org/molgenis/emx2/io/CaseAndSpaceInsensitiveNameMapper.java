package org.molgenis.emx2.io;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.NameMapper;
import org.molgenis.emx2.TableMetadata;

/**
 * will map case insensitve and space insensitive names to the internal strict name (using cache to
 * prevent the more expensive search) todo: also map based on label + locale
 */
public class CaseAndSpaceInsensitiveNameMapper implements NameMapper {
  private final TableMetadata tableMetadata;
  private final Map<String, String> cache = new LinkedHashMap<>();

  public CaseAndSpaceInsensitiveNameMapper(TableMetadata tableMetadata) {
    this.tableMetadata = tableMetadata;
  }

  @Override
  public String map(String name) {
    if (!cache.containsKey(name)) {
      Optional<Column> result =
          tableMetadata.getMutationColumns().stream()
              .filter(
                  column -> strip(column.getName().toLowerCase()).equals(strip(name).toLowerCase()))
              .findFirst();
      if (result.isPresent()) {
        cache.put(name, result.get().getName());
      } else {
        cache.put(name, name);
      }
    }
    return cache.get(name);
  }

  private String strip(String name) {
    return name.toLowerCase().replace(" ", "");
  }
}
