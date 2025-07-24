RESET ROLE;
CREATE ROLE "MG_USER_test@test.com" WITH NOLOGIN;
CREATE ROLE "MG_USER_test2@test.com" WITH NOLOGIN;
GRANT "MG_USER_user" TO "MG_USER_test@test.com";
GRANT "MG_USER_user" TO "MG_USER_test2@test.com";
GRANT "MG_PERM:pet store:_ALL_:SELECT" TO "MG_USER_test@test.com"; -- this should done via a trigger function right?


INSERT INTO "MOLGENIS".users_metadata (username, password, enabled, settings, admin)
VALUES ('test@test.com', null, true, '{}', false);
INSERT INTO "MOLGENIS".users_metadata (username, password, enabled, settings, admin)
VALUES ('test2@test.com', null, true, '{}', false);

UPDATE "MOLGENIS".group_metadata
SET users = '{test@test.com, test2@test.com}'
WHERE group_name LIKE '"pet store"#_VIEWER' ESCAPE '#';

INSERT INTO "MOLGENIS".group_metadata (group_name, group_description, users)
VALUES ('pet_store_special', 'pet store special', '{test@test.com}');
INSERT INTO "MOLGENIS".group_permissions (group_name, table_schema, table_name, has_select, has_insert, has_update,
                                          has_delete, has_group_select, has_group_update, has_group_delete, has_admin)
VALUES ('pet_store_special', 'pet store', 'Pet', true, true, true,
        false, true, true, false, false);
CREATE ROLE "pet_store_special"; -- needed for user_permissions mv
CREATE ROLE """pet_store""_VIEWER";
-- needs _ALL_ column in table_metadata: remove fk or add it by default?
INSERT INTO "MOLGENIS".table_metadata (table_schema, table_name) VALUES ('pet store', '_ALL_');
SELECT "MOLGENIS".create_or_update_schema_groups('pet store');
SELECT "MOLGENIS".enable_RLS_on_table('pet store', 'Pet');

UPDATE "pet store"."Pet"
SET mg_group = 'pet_store_special'
WHERE name = 'fire ant';

SET ROLE "MG_USER_test@test.com";
SELECT COUNT(*) FROM "pet store"."Pet";
SELECT * FROM "pet store"."Pet";

SET ROLE "MG_USER_test2@test.com";
SELECT COUNT(*) FROM "pet store"."Pet";

