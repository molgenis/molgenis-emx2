INSERT INTO "MOLGENIS".users_metadata (username, password, enabled, settings, admin)
VALUES ('test@test.com'::varchar, null::varchar, true::boolean, '{}'::json, false::boolean);
INSERT INTO "MOLGENIS".group_metadata (group_name, group_description, users)
VALUES ('pet_store_special'::text, 'pet store special'::text, '{test@test.com}');
INSERT INTO "MOLGENIS".group_permissions (group_name, table_schema, table_name, has_select, has_insert, has_update,
                                          has_delete, has_group_select, has_group_update, has_group_delete, has_admin)
VALUES ('pet_store_special'::text, 'pet store'::text, 'Pet'::text, true::boolean, true::boolean, true::boolean,
        false::boolean, true::boolean, true::boolean, false::boolean, false::boolean);

SELECT "MOLGENIS".enable_RLS_on_table('pet store', 'Pet');
UPDATE "pet store"."Pet"
SET mg_group = 'pet_store_special'::varchar
WHERE name LIKE 'fire ant' ESCAPE '#';
SELECT COUNT(*) FROM "pet store"."Pet";