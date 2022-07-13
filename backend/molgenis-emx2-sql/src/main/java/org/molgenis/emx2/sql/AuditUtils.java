package org.molgenis.emx2.sql;

public class AuditUtils {

  private AuditUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static String buildProcessAuditFunction(String schemaName, String tableName) {
    return """
            CREATE OR REPLACE FUNCTION "%1$s"."process_%3$s_audit"() RETURNS TRIGGER AS $%3$s_audit$
                   BEGIN
                       IF (TG_OP = 'DELETE') THEN
                           INSERT INTO "%1$s".mg_changelog
                           SELECT 'D', now(), user, TG_TABLE_NAME, row_to_json(OLD.*), row_to_json(NEW.*);
                       ELSIF (TG_OP = 'UPDATE') THEN
                           INSERT INTO "%1$s".mg_changelog
                           SELECT 'U', now(), user, TG_TABLE_NAME, row_to_json(OLD.*), row_to_json(NEW.*);
                       ELSIF (TG_OP = 'INSERT') THEN
                           INSERT INTO "%1$s".mg_changelog
                           SELECT 'I', now(), user, TG_TABLE_NAME, row_to_json(OLD.*), row_to_json(NEW.*);
                       END IF;
                       RETURN NULL; -- result is ignored since this is an AFTER trigger
                   END;
                   $%3$s_audit$ LANGUAGE plpgsql;
                """
        .formatted(
            schemaName,
            AuditUtils.buildFunctionName(schemaName),
            AuditUtils.buildFunctionName(tableName));
  }

  public static String buildAuditTrigger(String schemaName, String tableName) {
    return """
            CREATE TRIGGER %3$s_audit
            AFTER INSERT OR UPDATE OR DELETE ON "%1$s"."%2$s"
                FOR EACH ROW EXECUTE FUNCTION "%1$s"."process_%3$s_audit"();
            """
        .formatted(schemaName, tableName, AuditUtils.buildFunctionName(tableName));
  }

  public static String buildAuditTriggerRemove(String schemaName, String tableName) {
    return """
            DROP TRIGGER IF EXISTS %3$s_audit ON "%1$s"."%2$s" CASCADE
            """
        .formatted(schemaName, tableName, AuditUtils.buildFunctionName(tableName));
  }

  public static String buildProcessAuditFunctionRemove(String schemaName, String tableName) {
    return """
            DROP FUNCTION IF EXISTS "%1$s"."process_%2$s_audit"() CASCADE
            """
        .formatted(schemaName, AuditUtils.buildFunctionName(tableName));
  }

  private static String buildFunctionName(String tableName) {
    return tableName.replace(" ", "_");
  }
}
