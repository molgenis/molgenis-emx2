package org.molgenis.emx2.sql;

import org.molgenis.emx2.AutoIdGenerator;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

public class SqlAutoIdGenerator implements AutoIdGenerator {

  @Override
  public String generate(Column column) {
    String id = SnowflakeIdGenerator.getInstance().generateId();
    if (column.getComputed() != null) {
      return column.getComputed().replace(Constants.COMPUTED_AUTOID_TOKEN, id);
    }
    return id;
  }
}
