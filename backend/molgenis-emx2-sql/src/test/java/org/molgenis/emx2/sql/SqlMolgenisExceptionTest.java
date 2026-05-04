package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

class SqlMolgenisExceptionTest {

  private static final String SQLSTATE_INSUFFICIENT_PRIVILEGE = "42501";
  private static final String CANNOT_CHANGE_OWNER = "cannot change row owner";
  private static final String CANNOT_CHANGE_GROUPS = "cannot change row groups";

  @Test
  void mgOwnerColumnDenied_update_translatesFriendlyMessage() {
    MolgenisException ex = new SqlMolgenisException(daeWithColumnDenied(Constants.MG_OWNER_COLUMN));
    assertTrue(
        ex.getMessage().contains(CANNOT_CHANGE_OWNER),
        "UPDATE mg_owner must produce '" + CANNOT_CHANGE_OWNER + "', got: " + ex.getMessage());
  }

  @Test
  void mgOwnerColumnDenied_insert_translatesFriendlyMessage() {
    MolgenisException ex = new SqlMolgenisException(daeWithColumnDenied(Constants.MG_OWNER_COLUMN));
    assertTrue(
        ex.getMessage().contains(CANNOT_CHANGE_OWNER),
        "INSERT mg_owner must produce '" + CANNOT_CHANGE_OWNER + "', got: " + ex.getMessage());
  }

  @Test
  void mgGroupsColumnDenied_update_translatesFriendlyMessage() {
    MolgenisException ex =
        new SqlMolgenisException(daeWithColumnDenied(Constants.MG_GROUPS_COLUMN));
    assertTrue(
        ex.getMessage().contains(CANNOT_CHANGE_GROUPS),
        "UPDATE mg_groups must produce '" + CANNOT_CHANGE_GROUPS + "', got: " + ex.getMessage());
  }

  @Test
  void mgGroupsColumnDenied_insert_translatesFriendlyMessage() {
    MolgenisException ex =
        new SqlMolgenisException(daeWithColumnDenied(Constants.MG_GROUPS_COLUMN));
    assertTrue(
        ex.getMessage().contains(CANNOT_CHANGE_GROUPS),
        "INSERT mg_groups must produce '" + CANNOT_CHANGE_GROUPS + "', got: " + ex.getMessage());
  }

  @Test
  void unrelatedColumnDenied_preservesOriginalPgMessage() {
    String columnName = "score";
    MolgenisException ex = new SqlMolgenisException(daeWithColumnDenied(columnName));
    assertFalse(
        ex.getMessage().contains(CANNOT_CHANGE_OWNER),
        "score column must not produce owner message, got: " + ex.getMessage());
    assertFalse(
        ex.getMessage().contains(CANNOT_CHANGE_GROUPS),
        "score column must not produce groups message, got: " + ex.getMessage());
    assertTrue(
        ex.getMessage().contains("permission denied for column " + columnName),
        "fall-through must preserve original message, got: " + ex.getMessage());
  }

  @Test
  void nonPrivilegeError_preservesOriginalPgMessage() {
    PSQLException psql =
        new PSQLException("syntax error at or near INSERT", PSQLState.SYNTAX_ERROR);
    DataAccessException dae = new DataAccessException("SQL [bad sql]; syntax error", psql);
    MolgenisException ex = new SqlMolgenisException(dae);
    assertFalse(
        ex.getMessage().contains(CANNOT_CHANGE_OWNER),
        "non-42501 error must not trigger owner translation, got: " + ex.getMessage());
  }

  private static DataAccessException daeWithColumnDenied(String columnName) {
    String pgWireMessage =
        "S"
            + "ERROR\0"
            + "C"
            + SQLSTATE_INSUFFICIENT_PRIVILEGE
            + "\0"
            + "M"
            + "permission denied for column "
            + columnName
            + "\0"
            + "c"
            + columnName
            + "\0"
            + "\0";
    PSQLException psql = new PSQLException(new ServerErrorMessage(pgWireMessage));
    return new DataAccessException("SQL [synthetic]; " + psql.getMessage(), psql);
  }
}
