package org.molgenis.emx2.sql;

import org.jooq.exception.DataAccessException;
import org.jooq.impl.DataSourceConnectionProvider;
import org.molgenis.emx2.utils.MolgenisException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;

public class UserAwareConnectionProvider extends DataSourceConnectionProvider {
  private String activeUser;

  public UserAwareConnectionProvider(DataSource source) {
    super(source);
  }

  @Override
  public Connection acquire() throws DataAccessException {
    Connection connection = super.acquire();
    if (activeUser != null) {
      try (Statement stmt = connection.createStatement()) {
        stmt.execute("SET SESSION AUTHORIZATION " + name(MG_USER_PREFIX + activeUser) + "");
      } catch (SQLException sqle) {
        throw new MolgenisException(
            "set active user failed",
            "set active user failed",
            "active user '" + activeUser + "' failed");
      }
    }
    return connection;
  }

  @Override
  public void release(Connection connection) throws DataAccessException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("RESET SESSION AUTHORIZATION");
    } catch (SQLException sqle) {
      throw new RuntimeException("release of connection failed ", sqle);
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
