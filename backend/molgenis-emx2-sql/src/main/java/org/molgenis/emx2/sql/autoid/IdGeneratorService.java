package org.molgenis.emx2.sql.autoid;

import java.util.regex.Pattern;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

public class IdGeneratorService {

  private static final Pattern FUNCTION_PATTERN = Pattern.compile("\\$\\{mg_autoid(\\([^)]*\\))?}");

  public Field<String> generateIdForColumn(Column column) {
    if (!ColumnType.AUTO_ID.equals(column.getColumnType())) {
      throw new MolgenisException("Column type needs to be " + ColumnType.AUTO_ID);
    }

    String computed = column.getComputed();

    if (computed == null) {
      return DSL.val(SnowflakeIdGenerator.getInstance().generateId());
    }

    validateComputed(computed);

    if (computed.contains(Constants.COMPUTED_AUTOID_TOKEN)) {
      String id = SnowflakeIdGenerator.getInstance().generateId();
      return DSL.val(computed.replace(Constants.COMPUTED_AUTOID_TOKEN, id));
    }

    DSLIdGenerator generator = new RetryingIdGenerator(column);
    return generator.generateId();
  }

  private static void validateComputed(String computed) {
    if (FUNCTION_PATTERN.matcher(computed).results().count() > 1) {
      throw new MolgenisException(
          "Cannot generate autoid for column "
              + computed
              + " because mg_autoid can only be used once");
    }
  }
}
