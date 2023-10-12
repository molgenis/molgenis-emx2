package org.molgenis.emx2.io;

import org.molgenis.emx2.NameMapper;
import org.molgenis.emx2.TableMetadata;

public class NameToLabelMapper implements NameMapper {
  TableMetadata tableMetadata;

  public NameToLabelMapper(TableMetadata tableMetadata) {
    this.tableMetadata = tableMetadata;
  }

  @Override
  public String map(String name) {
    return tableMetadata.getColumn(name).getLabels().get("en");
  }
}
