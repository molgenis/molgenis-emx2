package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;

import java.util.List;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;

public class SqlDatabaseUtils {

  static void executeDropSchema(DSLContext jooq, SchemaMetadata schema) {
    try {
      jooq.dropSchema(name(schema.getName())).cascade().execute();
      MetadataUtils.deleteSchema(jooq, schema);
    } catch (MolgenisException me) {
      throw new MolgenisException("Drop schema failed", me.getMessage());
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Drop schema failed", dae);
    }
  }

  static void executeAddUser(DSLContext jooq, String user) {
    try {
      String userName = MG_USER_PREFIX + user;
      List<Record> result =
          jooq.fetch("SELECT rolname FROM pg_catalog.pg_roles WHERE rolname = {0}", userName);
      if (result.isEmpty()) jooq.execute("CREATE ROLE {0} WITH NOLOGIN", name(userName));
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Add user failed", dae);
    }
  }

  static void executeGrantCreateSchema(DSLContext jooq, String user) {
    try {
      String databaseName = jooq.fetchOne("SELECT current_database()").get(0, String.class);
      jooq.execute(
          "GRANT CREATE ON DATABASE {0} TO {1}", name(databaseName), name(MG_USER_PREFIX + user));
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(dae);
    }
  }
}
