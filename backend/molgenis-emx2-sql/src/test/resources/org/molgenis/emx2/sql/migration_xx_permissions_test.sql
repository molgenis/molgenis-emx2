RESET ROLE;
CREATE ROLE "MG_USER_test@test.com" WITH NOLOGIN;
CREATE ROLE "MG_USER_test2@test.com" WITH NOLOGIN;
GRANT "MG_USER_user" TO "MG_USER_test@test.com";
GRANT "MG_USER_user" TO "MG_USER_test2@test.com";


INSERT INTO "MOLGENIS".users_metadata (username, password, enabled, settings, admin)
VALUES ('test@test.com', null, true, '{}', false);
INSERT INTO "MOLGENIS".users_metadata (username, password, enabled, settings, admin)
VALUES ('test2@test.com', null, true, '{}', false);

SET ROLE "MG_USER_test@test.com";
SELECT * FROM pet_store."Pet";

RESET ROLE;
INSERT INTO "MOLGENIS".group_metadata (group_name, group_description, users)
VALUES ('pet_store_SPECIAL', 'pet store special', '{test@test.com}');

INSERT INTO "MOLGENIS".group_permissions (group_name, table_schema, table_name, has_select, has_insert, has_update,
                                          has_delete, has_group_select, has_group_update, has_group_delete, has_admin)
VALUES ('pet_store_SPECIAL', 'pet_store', 'Pet', true, true, true,
        false, true, true, false, false);
SET ROLE "MG_USER_test@test.com";
SELECT * FROM pet_store."Pet";
SET ROLE "MG_USER_test2@test.com";
SELECT * FROM pet_store."Pet";

RESET ROLE;
SELECT "MOLGENIS".enable_rls_on_table('pet_store', 'Pet');

UPDATE "pet_store"."Pet"
SET mg_group = 'pet_store_SPECIAL'
WHERE name = 'fire ant';

grant usage on schema pet_store to "MG_ROLE_pet_store_VIEWER";-- todo: this should not needed but added by trigger
GRANT SELECT ON "pet_store"."Pet" TO "MG_ROLE_pet_store_VIEWER"; --todo: trigger fail?
GRANT "MG_ROLE_pet_store_VIEWER" TO "MG_USER_test2@test.com"; --todo: trigger fail?


SET ROLE "MG_USER_test@test.com";
SELECT COUNT(*) FROM "pet_store"."Pet";
SELECT * FROM "pet_store"."Pet";

UPDATE "MOLGENIS".group_metadata
SET users = '{test@test.com, test2@test.com}'
WHERE group_name = 'pet_store_VIEWER';

SET ROLE "MG_USER_test2@test.com";
SELECT COUNT(*) FROM "pet_store"."Pet";
SELECT * FROM "pet_store"."Pet";

