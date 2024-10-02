package org.molgenis.emx2.sql;

import static java.lang.Boolean.TRUE;

import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.utils.TypeUtils;

public class ChangeLogUtils {

  private ChangeLogUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static String buildProcessAuditFunction(String schemaName, String tableName) {
    return """
        CREATE OR REPLACE FUNCTION "%1$s"."process_%3$s_audit"()
        RETURNS TRIGGER AS $%3$s_audit$
        DECLARE
            old_row JSONB;
            new_row JSONB;
            col_name TEXT;
            old_value TEXT;
            new_value TEXT;
        BEGIN
            -- Initialize empty JSONB objects
            old_row := '{}'::JSONB;
            new_row := '{}'::JSONB;

            -- Loop through each column in the OLD record
            FOR col_name IN SELECT column_name
                             FROM information_schema.columns
                             WHERE table_name = TG_TABLE_NAME
            LOOP
                -- Skip columns that end with '_contents' or '_TEXT_SEARCH_COLUMN'
                IF col_name LIKE '%%_contents' OR col_name LIKE '%%_TEXT_SEARCH_COLUMN' OR col_name LIKE 'mg_%%' THEN
                    CONTINUE;
                END IF;
                IF TG_OP != 'INSERT' THEN
                    EXECUTE 'SELECT ($1).' || quote_ident(col_name) INTO old_value USING OLD;
                    IF old_value IS NOT NULL THEN
                      old_row := jsonb_set(old_row, ARRAY[col_name], to_jsonb(old_value::TEXT)::JSONB);
                    END IF;
                END IF;
                IF TG_OP != 'DELETE' THEN
                    EXECUTE 'SELECT ($1).' || quote_ident(col_name) INTO new_value USING NEW;
                    IF new_value IS NOT NULL THEN
                      new_row := jsonb_set(new_row, ARRAY[col_name], to_jsonb(new_value::TEXT)::JSONB);
                    END IF;
                END IF;
            END LOOP;

            -- Log the change based on the operation
            IF TG_OP = 'DELETE' THEN
                INSERT INTO "%1$s".mg_changelog
                SELECT 'D', now(), user, TG_TABLE_NAME, old_row, new_row;
            ELSIF TG_OP = 'UPDATE' THEN
                INSERT INTO "%1$s".mg_changelog
                SELECT 'U', now(), user, TG_TABLE_NAME, old_row, new_row;
            ELSIF TG_OP = 'INSERT' THEN
                INSERT INTO "%1$s".mg_changelog
                SELECT 'I', now(), user, TG_TABLE_NAME, old_row, new_row;
            END IF;

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

  public static boolean isChangeSchema(Database db, String schemaName) {
    return TRUE.equals(
        TypeUtils.toBool(
            db.getSchema(schemaName).getMetadata().getSetting(Constants.IS_CHANGELOG_ENABLED)));
  }

  private static String buildFunctionName(String tableName) {
    return tableName.replace(" ", "_");
  }
}
