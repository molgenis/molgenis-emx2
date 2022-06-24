package org.molgenis.emx2.sql;

public class AuditUtils {

  public static String buildProcessAuditFunction(String schemaName, String tableName) {
    return """
            CREATE OR REPLACE FUNCTION process_%3$s_audit() RETURNS TRIGGER AS $%2$s_audit$
                   BEGIN
                       --
                       -- Create rows in %3$s_audit to reflect the operations performed on emp,
                       -- making use of the special variable TG_OP to work out the operation.
                       --
                       IF (TG_OP = 'DELETE') THEN
                           INSERT INTO "%1$s".mg_changelog
                           SELECT 'D', now(), user, row_to_json(OLD.*), row_to_json(NEW.*);
                       ELSIF (TG_OP = 'UPDATE') THEN
                           INSERT INTO "%1$s".mg_changelog
                           SELECT 'U', now(), user, row_to_json(OLD.*), row_to_json(NEW.*);
                       ELSIF (TG_OP = 'INSERT') THEN
                           INSERT INTO "%1$s".mg_changelog
                           SELECT 'I', now(), user, row_to_json(OLD.*), row_to_json(NEW.*);
                       END IF;
                       RETURN NULL; -- result is ignored since this is an AFTER trigger
                   END;
                   $%3$s_audit$ LANGUAGE plpgsql;
                """
        .formatted(schemaName, tableName, AuditUtils.buildFunctionName(tableName));
  }

  public static String buildAuditTrigger(String schemaName, String tableName) {
    return """
            CREATE TRIGGER %3$s_audit
            AFTER INSERT OR UPDATE OR DELETE ON "%1$s"."%2$s"
                FOR EACH ROW EXECUTE FUNCTION process_%3$s_audit();
            """
        .formatted(schemaName, tableName, AuditUtils.buildFunctionName(tableName));
  }

  private static String buildFunctionName(String tableName) {
    return tableName.replace(" ", "_");
  }
}
