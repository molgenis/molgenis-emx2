package org.molgenis.emx2.sql;

import static java.lang.Boolean.TRUE;

import org.molgenis.emx2.Constants;
import org.molgenis.emx2.SchemaMetadataProvider;
import org.molgenis.emx2.utils.TypeUtils;

public class ChangeLogUtils {

  private ChangeLogUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static String buildProcessAuditFunction(String schemaName, String tableName) {
    return """
            CREATE OR REPLACE FUNCTION "%1$s"."process_%3$s_audit"() RETURNS TRIGGER AS $%3$s_audit$
                   DECLARE
                       old_row jsonb := to_jsonb(OLD);
                       new_row jsonb := to_jsonb(NEW);
                       content_key text;
                   BEGIN
                       IF (TG_OP = 'UPDATE' AND old_row - 'mg_updatedOn' - 'mg_updatedBy' = new_row - 'mg_updatedOn' - 'mg_updatedBy') THEN
                           RETURN NULL;
                       END IF;
                       FOR content_key IN
                           SELECT k FROM jsonb_object_keys(COALESCE(old_row, new_row)) AS k
                           WHERE right(k, 9) = '_contents'
                             AND jsonb_exists(COALESCE(old_row, new_row), left(k, -9) || '_mimetype')
                       LOOP
                           old_row := old_row - content_key;
                           new_row := new_row - content_key;
                       END LOOP;
                       INSERT INTO "%1$s".mg_changelog
                       SELECT left(TG_OP, 1), now(), user, TG_TABLE_NAME, CAST(old_row AS json), CAST(new_row AS json);
                       RETURN NULL; -- result is ignored since this is an AFTER trigger
                   END;
                   $%3$s_audit$ LANGUAGE plpgsql;
                """
        .formatted(
            schemaName,
            ChangeLogUtils.buildFunctionName(schemaName),
            ChangeLogUtils.buildFunctionName(tableName));
  }

  public static String buildAuditTrigger(String schemaName, String tableName) {
    return """
            CREATE TRIGGER %3$s_audit
            AFTER INSERT OR UPDATE OR DELETE ON "%1$s"."%2$s"
                FOR EACH ROW EXECUTE FUNCTION "%1$s"."process_%3$s_audit"();
            """
        .formatted(schemaName, tableName, ChangeLogUtils.buildFunctionName(tableName));
  }

  public static String buildAuditTriggerRemove(String schemaName, String tableName) {
    return """
            DROP TRIGGER IF EXISTS %3$s_audit ON "%1$s"."%2$s" CASCADE
            """
        .formatted(schemaName, tableName, ChangeLogUtils.buildFunctionName(tableName));
  }

  public static String buildProcessAuditFunctionRemove(String schemaName, String tableName) {
    return """
            DROP FUNCTION IF EXISTS "%1$s"."process_%2$s_audit"() CASCADE
            """
        .formatted(schemaName, ChangeLogUtils.buildFunctionName(tableName));
  }

  public static boolean isChangeSchema(SchemaMetadataProvider db, String schemaName) {
    return TRUE.equals(
        TypeUtils.toBool(
            db.getSchemaMetadata(schemaName).getSetting(Constants.IS_CHANGELOG_ENABLED)));
  }

  private static String buildFunctionName(String tableName) {
    return tableName.replace(" ", "_");
  }
}
