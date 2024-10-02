package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChangeLogUtilsTest {
  @Test
  void testBuildProcessAuditFunction() {
    String expectedFunction =
        """
CREATE OR REPLACE FUNCTION "pet store"."process_Pet_audit"()
RETURNS TRIGGER AS $Pet_audit$
DECLARE
    old_row JSONB;
    new_row JSONB;
    col_name TEXT; -- Renamed to avoid ambiguity
BEGIN
    -- Initialize empty JSONB objects
    old_row := '{}'::JSONB;
    new_row := '{}'::JSONB;

    -- Loop through each column in the OLD record
    FOR col_name IN SELECT column_name
                     FROM information_schema.columns
                     WHERE table_name = TG_TABLE_NAME
    LOOP
        -- Skip columns that end with '_content' or '_TEXT_SEARCH_COLUMN'
        IF col_name LIKE '%_content' OR col_name LIKE '%_TEXT_SEARCH_COLUMN' THEN
            CONTINUE;
        END IF;

        -- Handle the different operation types
        IF TG_OP = 'INSERT' THEN
            -- For INSERT, include all columns
            new_row := jsonb_set(new_row, ARRAY[col_name], to_jsonb(NEW.*)::JSONB -> col_name);

        ELSIF TG_OP = 'UPDATE' AND (OLD.*)::JSONB -> col_name IS DISTINCT FROM (NEW.*)::JSONB -> col_name THEN
            -- For UPDATE, include only changed columns
            new_row := jsonb_set(new_row, ARRAY[col_name], to_jsonb(NEW.*)::JSONB -> col_name);
            old_row := jsonb_set(old_row, ARRAY[col_name], to_jsonb(OLD.*)::JSONB -> col_name);

        ELSIF TG_OP = 'DELETE' THEN
            -- For DELETE, include the current old record
            old_row := jsonb_set(old_row, ARRAY[col_name], to_jsonb(OLD.*)::JSONB -> col_name);
        END IF;

    END LOOP;

    -- Log the change based on the operation
    IF TG_OP = 'DELETE' THEN
        INSERT INTO "pet store".mg_changelog
        SELECT 'D', now(), user, TG_TABLE_NAME, row_to_json(old_row), NULL;

    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO "pet store".mg_changelog
        SELECT 'U', now(), user, TG_TABLE_NAME, old_row, new_row;

    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO "pet store".mg_changelog
        SELECT 'I', now(), user, TG_TABLE_NAME, NULL, new_row;
    END IF;

    RETURN NULL; -- result is ignored since this is an AFTER trigger
END;
$Pet_audit$ LANGUAGE plpgsql;
""";
    assertEquals(
        expectedFunction.strip(),
        ChangeLogUtils.buildProcessAuditFunction("pet store", "Pet").strip());
  }

  @Test
  void testBuildAuditTrigger() {
    String expectedTrigger =
        """
                        CREATE TRIGGER Pet_audit
                        AFTER INSERT OR UPDATE OR DELETE ON "pet store"."Pet"
                            FOR EACH ROW EXECUTE FUNCTION "pet store"."process_Pet_audit"();
                          """;
    assertEquals(
        expectedTrigger.strip(), ChangeLogUtils.buildAuditTrigger("pet store", "Pet").strip());
  }

  @Test
  void testBuildAuditTriggerWithSpaceInTableName() {
    String expectedTrigger =
        """
                        CREATE TRIGGER My_pets_audit
                        AFTER INSERT OR UPDATE OR DELETE ON "pet store"."My pets"
                            FOR EACH ROW EXECUTE FUNCTION "pet store"."process_My_pets_audit"();
                          """;
    assertEquals(
        expectedTrigger.strip(), ChangeLogUtils.buildAuditTrigger("pet store", "My pets").strip());
  }

  @Test
  void testRemoveProcessAuditFunction() {
    assertEquals(
        "DROP FUNCTION IF EXISTS \"my schema\".\"process_my_table_audit\"() CASCADE",
        ChangeLogUtils.buildProcessAuditFunctionRemove("my schema", "my table").strip());
  }

  @Test
  void testRemoveAuditTrigger() {
    assertEquals(
        "DROP TRIGGER IF EXISTS my_table_audit ON \"my schema\".\"my table\" CASCADE",
        ChangeLogUtils.buildAuditTriggerRemove("my schema", "my table").strip());
  }
}
