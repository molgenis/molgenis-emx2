package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChangeLogUtilsTest {
  @Test
  void testBuildProcessAuditFunction() {
    String expectedFunction =
        """
                        CREATE OR REPLACE FUNCTION "pet store"."process_Pet_audit"() RETURNS TRIGGER AS $Pet_audit$
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
                                   INSERT INTO "pet store".mg_changelog
                                   SELECT left(TG_OP, 1), now(), user, TG_TABLE_NAME, CAST(old_row AS json), CAST(new_row AS json);
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
