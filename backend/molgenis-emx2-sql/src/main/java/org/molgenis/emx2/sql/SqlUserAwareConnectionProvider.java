package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.Constants.MG_USER_PREFIX;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import java.sql.Connection;
import javax.sql.DataSource;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;

public class SqlUserAwareConnectionProvider extends DataSourceConnectionProvider {
  private String activeUser;
  private boolean isAdmin = false;
  private String rlsActiveRole = "";
  private String rlsSelectTables = "";
  private String rlsInsertTables = "";
  private String rlsUpdateTables = "";
  private String rlsDeleteTables = "";

  public SqlUserAwareConnectionProvider(DataSource source) {
    super(source);
  }

  @Override
  public Connection acquire() {
    Connection connection = null;
    try {
      connection = super.acquire();
      if (getActiveUser().equals(ADMIN_USER) || this.isAdmin) {
        DSL.using(connection, SQLDialect.POSTGRES)
            .execute(
                "RESET ROLE; SET jit='off';"
                    + " SET molgenis.active_role = {0};"
                    + " SET molgenis.rls_select_tables = {1};"
                    + " SET molgenis.rls_insert_tables = {2};"
                    + " SET molgenis.rls_update_tables = {3};"
                    + " SET molgenis.rls_delete_tables = {4}",
                inline(rlsActiveRole),
                inline(rlsSelectTables),
                inline(rlsInsertTables),
                inline(rlsUpdateTables),
                inline(rlsDeleteTables));
      } else {
        DSL.using(connection, SQLDialect.POSTGRES)
            .execute(
                "RESET ROLE; SET jit='off'; SET ROLE {0};"
                    + " SET molgenis.active_role = {1};"
                    + " SET molgenis.rls_select_tables = {2};"
                    + " SET molgenis.rls_insert_tables = {3};"
                    + " SET molgenis.rls_update_tables = {4};"
                    + " SET molgenis.rls_delete_tables = {5}",
                name(MG_USER_PREFIX + getActiveUser()),
                inline(rlsActiveRole),
                inline(rlsSelectTables),
                inline(rlsInsertTables),
                inline(rlsUpdateTables),
                inline(rlsDeleteTables));
      }
      return connection;
    } catch (DataAccessException dae) {
      super.release(connection);
      throw new SqlMolgenisException("Set active user failed'", dae);
    }
  }

  @Override
  public void release(Connection connection) {
    try {
      DSL.using(connection, SQLDialect.POSTGRES)
          .execute(
              "RESET ROLE; RESET search_path;"
                  + " SET molgenis.active_role = '';"
                  + " SET molgenis.rls_select_tables = '';"
                  + " SET molgenis.rls_insert_tables = '';"
                  + " SET molgenis.rls_update_tables = '';"
                  + " SET molgenis.rls_delete_tables = ''");
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("release of connection failed ", dae);
    }
    super.release(connection);
  }

  public String getActiveUser() {
    // default user is ANONYMOUS, not admin! (never null)
    return activeUser == null ? ANONYMOUS : activeUser;
  }

  public void setActiveUser(String activeUser) {
    this.activeUser = activeUser;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean admin) {
    isAdmin = admin;
  }

  public void clearActiveUser() {
    this.activeUser = null;
    this.isAdmin = false;
  }

  public void setRlsSessionVars(
      String activeRole,
      String selectTables,
      String insertTables,
      String updateTables,
      String deleteTables) {
    this.rlsActiveRole = activeRole;
    this.rlsSelectTables = selectTables;
    this.rlsInsertTables = insertTables;
    this.rlsUpdateTables = updateTables;
    this.rlsDeleteTables = deleteTables;
  }

  public void clearRlsCache() {
    this.rlsActiveRole = "";
    this.rlsSelectTables = "";
    this.rlsInsertTables = "";
    this.rlsUpdateTables = "";
    this.rlsDeleteTables = "";
  }

  public String getRlsActiveRole() {
    return rlsActiveRole;
  }

  public String getRlsSelectTables() {
    return rlsSelectTables;
  }

  public String getRlsInsertTables() {
    return rlsInsertTables;
  }

  public String getRlsUpdateTables() {
    return rlsUpdateTables;
  }

  public String getRlsDeleteTables() {
    return rlsDeleteTables;
  }
}
