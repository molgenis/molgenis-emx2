package org.molgenis.emx2.sql.autoid;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Operator;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.DataException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.MolgenisException;

public class SqlSequence {

  private final DSLContext jooq;
  private final String name;
  private final String schema;

  public SqlSequence(DSLContext jooq, String schema, String name) {
    this.jooq = jooq;
    this.name = name;
    this.schema = schema;
  }

  public static SqlSequence create(DSLContext jooq, String schema, String name, long limit) {
    jooq.createSequence(DSL.name(schema, name))
        .maxvalue(limit)
        .startWith(1)
        .incrementBy(1)
        .execute();
    return new SqlSequence(jooq, schema, name);
  }

  public static boolean exists(DSLContext jooq, String schema, String name) {
    return jooq.selectOne()
        .from("information_schema.sequences")
        .where(
            DSL.condition(
                Operator.AND,
                DSL.field("sequence_schema").eq(schema),
                DSL.field("sequence_name").eq(name)))
        .fetch()
        .isNotEmpty();
  }

  public long currentValue() {
    try {
      Long currentValue =
          jooq.select(
                  DSL.field("pg_sequence_last_value('" + DSL.name(schema, name) + "')", Long.class))
              .fetchOne()
              .value1();

      if (currentValue == null) {
        return 0;
      }

      return currentValue;
    } catch (DataAccessException e) {
      throw new MolgenisException("No sequence with name: " + name + " for schema: " + schema);
    }
  }

  public long nextValue() {
    try {
      return jooq.select(DSL.field("nextval('" + DSL.name(schema, name) + "')", Long.class))
          .fetchOne()
          .value1();
    } catch (DataException e) {
      throw new MolgenisException("Unable to generate value for sequence");
    } catch (DataAccessException e) {
      throw new MolgenisException("No sequence with name: " + name + " for schema: " + schema);
    }
  }

  public long limit() {
    List<Long> limit =
        jooq.select(DSL.field("maximum_value"))
            .from("information_schema.sequences")
            .where(
                DSL.condition(
                    Operator.AND,
                    DSL.field("sequence_schema").eq(schema),
                    DSL.field("sequence_name").eq(name)))
            .fetch(DSL.field("maximum_value"), Long.class);

    if (limit.isEmpty()) {
      throw new MolgenisException(
          "No sequence with name: " + name + " defined for schema: " + schema);
    }

    return limit.getFirst();
  }

  public void delete() {
    jooq.dropSequence(DSL.name(schema, name)).execute();
  }
}
