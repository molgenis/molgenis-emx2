package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;

import java.sql.Connection;
import javax.sql.DataSource;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;

public class SqlUserAwareConnectionProvider extends DataSourceConnectionProvider {
  private String activeUser;

  public SqlUserAwareConnectionProvider(DataSource source) {
    super(source);
  }

  @Override
  public Connection acquire() {
    try {
      Connection connection = super.acquire();
      if (activeUser != null) {
        DSL.using(connection, SQLDialect.POSTGRES)
            .execute("SET SESSION AUTHORIZATION {0}", name(MG_USER_PREFIX + activeUser));
      }
      return connection;
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Set active user failed'", dae);
    }
  }

  @Override
  public void release(Connection connection) {
    try {
      DSL.using(connection, SQLDialect.POSTGRES).execute("RESET SESSION AUTHORIZATION");
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("release of connection failed ", dae);
    }
    super.release(connection);
  }

  public String getActiveUser() {
    return activeUser;
  }

  public void setActiveUser(String activeUser) {
    this.activeUser = activeUser;
  }

  public void clearActiveUser() {
    this.activeUser = null;
  }
}
