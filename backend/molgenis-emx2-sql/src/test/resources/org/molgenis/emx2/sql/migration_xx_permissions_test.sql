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
SELECT *
FROM pet_store."Pet";

RESET ROLE;
INSERT INTO "MOLGENIS".group_metadata (group_name, group_description, users)
VALUES ('pet_store_SPECIAL', 'pet store special', '{test@test.com}');

INSERT INTO "MOLGENIS".group_permissions (group_name, table_schema, table_name, has_select, has_insert, has_update,
                                          has_delete, has_admin)
VALUES ('pet_store_SPECIAL', 'pet_store', 'Pet', true, true, true, false, false);

-- test@test is part of pet_store_SPECIAL and can select all rows
SET ROLE "MG_USER_test@test.com";
SELECT *
FROM pet_store."Pet";

-- test2@test is not and cannot select any rows
SET ROLE "MG_USER_test2@test.com";
SELECT *
FROM pet_store."Pet";

RESET ROLE;
-- Enable row level security will disable all select rights, TODO: maybe add some by default to trigger function?
SELECT "MOLGENIS".enable_rls_on_table('pet_store', 'Pet');

-- Set group to a row
UPDATE "pet_store"."Pet"
SET mg_group = '{pet_store_SPECIAL}' -- converted this to an array
WHERE name = 'fire ant';


SET ROLE "MG_USER_test@test.com";
-- test@test.com part of the pet_store_SPECIAL can no only see this one row.
SELECT COUNT(*)
FROM "pet_store"."Pet";
SELECT *
FROM "pet_store"."Pet";

-- Still no access
SET ROLE "MG_USER_test2@test.com";
SELECT *
FROM "pet_store"."Pet";

RESET ROLE;
UPDATE "MOLGENIS".group_metadata -- TODO: bug a non admin user can do this?
SET users = '{test@test.com, test2@test.com}'
WHERE group_name = 'pet_store_VIEWER';
GRANT "MG_ROLE_pet_store_VIEWER" TO "MG_USER_test2@test.com"; --todo: trigger fail?
GRANT SELECT ON "pet_store"."Pet" TO "MG_ROLE_pet_store_VIEWER"; --todo: trigger fail?


SET ROLE "MG_USER_test2@test.com";
-- No rows with access
SELECT *
FROM "pet_store"."Pet";

RESET ROLE;
-- Give access to the 7 other rows
UPDATE "pet_store"."Pet"
SET mg_group = ARRAY['pet_store_VIEWER']
WHERE mg_group IS NULL;
REFRESH MATERIALIZED VIEW "MOLGENIS".user_permissions_mv; -- this should be triggerd


SET ROLE "MG_USER_test2@test.com";
SELECT *
FROM "pet_store"."Pet";
