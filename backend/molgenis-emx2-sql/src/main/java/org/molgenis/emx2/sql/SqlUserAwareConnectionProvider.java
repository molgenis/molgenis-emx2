package org.molgenis.emx2.sql;

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
import org.molgenis.emx2.Username;

public class SqlUserAwareConnectionProvider extends DataSourceConnectionProvider {

  private Username activeUsername;
  private boolean isAdmin = false;

  public SqlUserAwareConnectionProvider(DataSource source) {
    super(source);
  }

  @Override
  public Connection acquire() {
    Connection connection = null;
    try {
      connection = super.acquire();
      if (ADMIN_USER.equals(getActiveUsername()) || this.isAdmin) {
        // as admin you are actually session user
        DSL.using(connection, SQLDialect.POSTGRES).execute("RESET ROLE; SET jit='off';");
      } else {
        // as non admin you are a current user
        DSL.using(connection, SQLDialect.POSTGRES)
            .execute(
                "RESET ROLE; SET jit='off'; SET ROLE {0}",
                name(MG_USER_PREFIX + getActiveUsername().value()));
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
      DSL.using(connection, SQLDialect.POSTGRES).execute("RESET ROLE");
      // sql reports might have changes this, therefore ensure always reset
      DSL.using(connection, SQLDialect.POSTGRES).execute("RESET search_path");
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("release of connection failed ", dae);
    }
    super.release(connection);
  }

  public Username getActiveUsername() {
    // default user is ANONYMOUS, not admin! (never null)
    return activeUsername == null ? ANONYMOUS : activeUsername;
  }

  public void setActiveUsername(Username activeUsername) {
    this.activeUsername = activeUsername;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean admin) {
    isAdmin = admin;
  }

  public void clearActiveUser() {
    this.activeUsername = null;
    this.isAdmin = false;
  }
}
