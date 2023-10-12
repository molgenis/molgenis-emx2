package org.molgenis.emx2.io;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.NameMapper;
import org.molgenis.emx2.TableMetadata;

public class LabelToNameMapper implements NameMapper {
  private TableMetadata tableMetadata;

  public LabelToNameMapper(TableMetadata tableMetadata) {
    this.tableMetadata = tableMetadata;
  }

  @Override
  public String map(String label) {
    for (Column c : tableMetadata.getColumns()) {
      if (c.getLabels().get("en").equals(label)
          || label.toLowerCase().replace(" ", "").equals(c.getName().toLowerCase())) {
        return c.getName();
      }
    }
    return null;
  }
}
