CREATE ROLE "MG_USER_test@test.com" WITH NOLOGIN;
GRANT "MG_USER_user" TO "MG_USER_test@test.com";

INSERT INTO "MOLGENIS".users_metadata (username, password, enabled, settings, admin)
VALUES ('test@test.com', null, true, '{}', false);
INSERT INTO "MOLGENIS".group_metadata (group_name, group_description, users)
VALUES ('pet_store_special', 'pet store special', '{test@test.com}');
INSERT INTO "MOLGENIS".group_permissions (group_name, table_schema, table_name, has_select, has_insert, has_update,
                                          has_delete, has_group_select, has_group_update, has_group_delete, has_admin)
VALUES ('pet_store_special', 'pet store', 'Pet', true, true, true,
        false, true, true, false, false);

SELECT "MOLGENIS".create_or_update_schema_groups('pet store');
SELECT "MOLGENIS".enable_RLS_on_table('pet store', 'Pet');

SET ROLE "MG_USER_test@test.com";
UPDATE "pet store"."Pet"
SET mg_group = 'pet_store_special'
WHERE name = 'fire ant';

SELECT COUNT(*) FROM "pet store"."Pet";