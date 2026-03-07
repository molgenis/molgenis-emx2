package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;

class PostgresVersionTest {

  @Test
  void givenVersion_thenSupported() {
    PostgresVersion version = PostgresVersion.fromDslContext(new SqlDatabase(false).getJooq());
    assertEquals(15, version.majorVersion());
    assertTrue(version.isSupported());
  }

  @Test
  void whenNoVersionReturned_thenThrow() {
    DSLContext context = getContextForVersion(null);
    assertThrows(MolgenisException.class, () -> PostgresVersion.fromDslContext(context));
  }

  @Test
  void givenUnsupportedVersion_thenNotSupported() {
    DSLContext context = getContextForVersion(42 * 10_000);
    PostgresVersion version = PostgresVersion.fromDslContext(context);
    assertFalse(version.isSupported());
  }

  private DSLContext getContextForVersion(Integer version) {
    MockDataProvider provider =
        ctx -> {
          DSLContext dsl = DSL.using(SQLDialect.POSTGRES);

          Field<Integer> field = DSL.field("version", Integer.class);
          Result<Record1<Integer>> result = dsl.newResult(field);
          result.add(dsl.newRecord(field).values(version));

          return new MockResult[] {new MockResult(1, result)};
        };

    return DSL.using(new MockConnection(provider), SQLDialect.POSTGRES);
  }
}
