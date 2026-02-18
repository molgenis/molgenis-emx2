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
  private String rlsBypassSelect = "";
  private String rlsBypassInsert = "";
  private String rlsBypassUpdate = "";
  private String rlsBypassDelete = "";

  public SqlUserAwareConnectionProvider(DataSource source) {
    super(source);
  }

  @Override
  public Connection acquire() {
    Connection connection = null;
    try {
      connection = super.acquire();
      if (getActiveUser().equals(ADMIN_USER) || this.isAdmin) {
        // as admin you are actually session user
        DSL.using(connection, SQLDialect.POSTGRES)
            .execute(
                "RESET ROLE; SET jit='off';"
                    + " SET molgenis.active_role = {0};"
                    + " SET molgenis.rls_bypass_select = {1};"
                    + " SET molgenis.rls_bypass_insert = {2};"
                    + " SET molgenis.rls_bypass_update = {3};"
                    + " SET molgenis.rls_bypass_delete = {4}",
                inline(rlsActiveRole),
                inline(rlsBypassSelect),
                inline(rlsBypassInsert),
                inline(rlsBypassUpdate),
                inline(rlsBypassDelete));
      } else {
        // as non admin you are a current user
        DSL.using(connection, SQLDialect.POSTGRES)
            .execute(
                "RESET ROLE; SET jit='off'; SET ROLE {0};"
                    + " SET molgenis.active_role = {1};"
                    + " SET molgenis.rls_bypass_select = {2};"
                    + " SET molgenis.rls_bypass_insert = {3};"
                    + " SET molgenis.rls_bypass_update = {4};"
                    + " SET molgenis.rls_bypass_delete = {5}",
                name(MG_USER_PREFIX + getActiveUser()),
                inline(rlsActiveRole),
                inline(rlsBypassSelect),
                inline(rlsBypassInsert),
                inline(rlsBypassUpdate),
                inline(rlsBypassDelete));
      }
      return connection;
    } catch (DataAccessException dae) {
      super.release(connection);
      // if invalid user we will not return a connection, not even anonymous
      throw new SqlMolgenisException("Set active user failed'", dae);
    }
  }

  @Override
  public void release(Connection connection) {
    try {
      // sql reports might have changes this, therefore ensure always reset
      DSL.using(connection, SQLDialect.POSTGRES)
          .execute(
              "RESET ROLE; RESET search_path;"
                  + " SET molgenis.active_role = '';"
                  + " SET molgenis.rls_bypass_select = '';"
                  + " SET molgenis.rls_bypass_insert = '';"
                  + " SET molgenis.rls_bypass_update = '';"
                  + " SET molgenis.rls_bypass_delete = ''");
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
      String selectBypass,
      String insertBypass,
      String updateBypass,
      String deleteBypass) {
    this.rlsActiveRole = activeRole;
    this.rlsBypassSelect = selectBypass;
    this.rlsBypassInsert = insertBypass;
    this.rlsBypassUpdate = updateBypass;
    this.rlsBypassDelete = deleteBypass;
  }

  public void clearRlsCache() {
    this.rlsActiveRole = "";
    this.rlsBypassSelect = "";
    this.rlsBypassInsert = "";
    this.rlsBypassUpdate = "";
    this.rlsBypassDelete = "";
  }

  public String getRlsActiveRole() {
    return rlsActiveRole;
  }

  public String getRlsBypassSelect() {
    return rlsBypassSelect;
  }

  public String getRlsBypassInsert() {
    return rlsBypassInsert;
  }

  public String getRlsBypassUpdate() {
    return rlsBypassUpdate;
  }

  public String getRlsBypassDelete() {
    return rlsBypassDelete;
  }
}
