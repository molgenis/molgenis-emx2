package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;

class SqlMolgenisExceptionTest {

  private static final String TITLE = "Transaction failed";

  @Test
  void preservesCause() {
    Exception cause = new IllegalStateException("boom");

    SqlMolgenisException exception = new SqlMolgenisException(TITLE, cause);

    assertEquals("Transaction failed: boom", exception.getMessage());
    assertSame(cause, exception.getCause());
    assertTrue(exception.getCause().getStackTrace().length > 0);
  }

  @Test
  void preservesDataAccessExceptionCause() {
    DataAccessException cause = new DataAccessException("relation does not exist");

    SqlMolgenisException exception = new SqlMolgenisException(TITLE, cause);

    assertEquals("Transaction failed: relation does not exist. ", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  @Test
  void preservesPostgresCause() {
    DataAccessException cause =
        new DataAccessException("wrapper", new PSQLException("psql boom", null));

    SqlMolgenisException exception = new SqlMolgenisException(TITLE, cause);

    assertEquals("Transaction failed: psql boom. ", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  @Test
  void preservesCauseWhenOnlyDataAccessExceptionGiven() {
    DataAccessException cause = new DataAccessException("relation does not exist");

    SqlMolgenisException exception = new SqlMolgenisException(cause);

    assertEquals("relation does not exist. ", exception.getMessage());
    assertSame(cause, exception.getCause());
  }
}
