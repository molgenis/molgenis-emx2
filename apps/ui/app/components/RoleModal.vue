<script setup lang="ts">
import { computed, ref, watch } from "vue";
import BaseIcon from "../../../tailwind-components/app/components/BaseIcon.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Field from "../../../tailwind-components/app/components/Field.vue";
import FormError from "../../../tailwind-components/app/components/form/Error.vue";
import InputCheckbox from "../../../tailwind-components/app/components/input/Checkbox.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import Table from "../../../tailwind-components/app/components/Table.vue";
import TableCell from "../../../tailwind-components/app/components/TableCell.vue";
import TableHead from "../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../tailwind-components/app/components/TableRow.vue";
import { errorToMessage } from "../../../tailwind-components/app/utils/errorToMessage";
import type { TablePermission } from "../../../tailwind-components/types/types.ts";
import useAdminSettings from "../composables/useAdminSettings.ts";
import type { CustomRole, RoleModalMode } from "../interfaces/interfaces.ts";
import { getRootTableNames } from "../util/adminUtils";

const RIGHTS = ["select", "insert", "update", "delete"] as const;
const RIGHT_LABELS: Record<Right, string> = {
  select: "Select",
  insert: "Insert",
  update: "Update",
  delete: "Delete",
};
const SYSTEM_ROLE_NAMES = [
  "Exists",
  "Range",
  "Aggregator",
  "Count",
  "Viewer",
  "Editor",
  "Manager",
  "Owner",
  "Member",
];
const RLS_ROLE_PREFIX = "RLS_";

const props = withDefaults(
  defineProps<{
    mode: RoleModalMode;
    role?: CustomRole;
  }>(),
  {
    mode: "create",
  }
);

const visible = defineModel<boolean>("visible", {
  required: true,
});

const adminSettings = await useAdminSettings();

const isEditing = computed(() => props.mode === "edit" && !!props.role);
const title = computed(() => {
  if (props.role && props.mode === "edit") {
    return `Edit role: ${props.role.role}`;
  } else if (props.role && props.mode === "copy") {
    return `Copy role: ${props.role.role}`;
  } else {
    return "Create role";
  }
});
const originalUsers = new Set(isEditing.value ? props.role?.users : []);

const roleName = ref(initialRoleName());
const schemaId = ref<string | null>(props.role?.schemaId ?? null);
const tableNames = ref<string[]>([]);
const originalPermissions = ref<Record<string, TablePermission>>({});
const permissions = ref<Record<string, TablePermission>>({});
const loadingTables = ref(false);
const userSearch = ref("");
const selectedUsers = ref<string[]>(Array.from(originalUsers));
const errorMessage = ref("");
const saving = ref(false);

const schemaIds = computed(() =>
  adminSettings.schemas.map((schema) => schema.id)
);

const trimmedRoleName = computed(() => roleName.value.trim());

const otherCustomRolesInSchema = computed(() =>
  (adminSettings.customRoles ?? []).filter(
    (customRole) =>
      customRole.schemaId === schemaId.value &&
      !(isEditing.value && customRole.role === props.role?.role)
  )
);

const roleNameError = computed(() => {
  const name = trimmedRoleName.value;
  if (!name) {
    return "";
  }
  if (SYSTEM_ROLE_NAMES.includes(name)) {
    return `'${name}' is a system role name`;
  }
  if (name.startsWith(RLS_ROLE_PREFIX)) {
    return `Role names cannot start with '${RLS_ROLE_PREFIX}'`;
  }
  if (
    otherCustomRolesInSchema.value.some(
      (customRole) => customRole.role === name
    )
  ) {
    return `Role '${name}' already exists in schema '${schemaId.value}'`;
  }
  return "";
});

const customRoleByUser = computed(() =>
  otherCustomRolesInSchema.value.reduce(
    (rolesByUser: Record<string, string>, customRole: CustomRole) => {
      customRole.users.forEach((email: string) => {
        rolesByUser[email] = customRole.role;
      });
      return rolesByUser;
    },
    {}
  )
);

const matchingUserEmails = computed(() => {
  const search = userSearch.value.trim().toLowerCase();
  const emails = adminSettings.users.map((user) => user.email);
  return search
    ? emails.filter((email) => email.toLowerCase().includes(search))
    : emails;
});

const tablePermissions = computed(() =>
  tableNames.value
    .map((tableName) => permissions.value[tableName])
    .filter((permission): permission is TablePermission => !!permission)
);

const allTablesHaveAllRights = computed(
  () =>
    tablePermissions.value.length > 0 &&
    tablePermissions.value.every(hasAllRights)
);

const someTableHasRights = computed(() =>
  tablePermissions.value.some(hasSomeRights)
);

const changedPermissions = computed(() =>
  tablePermissions.value
    .map(effectivePermission)
    .filter(
      (permission) =>
        !isSamePermission(
          permission,
          originalPermissions.value[permission.table] ??
            noPermissions(permission.table)
        )
    )
);

const addedUsers = computed(() =>
  Array.from(selectedUsers.value).filter((email) => !originalUsers.has(email))
);

const removedUsers = computed(() =>
  Array.from(originalUsers).filter(
    (email) => !selectedUsers.value.includes(email)
  )
);

const overwrittenRoles = computed(() =>
  addedUsers.value
    .filter((email) => customRoleByUser.value[email])
    .map((email) => ({ email, role: customRoleByUser.value[email]! }))
);

const hasChanges = computed(
  () =>
    changedPermissions.value.length ||
    addedUsers.value.length ||
    removedUsers.value.length > 0
);

const canSave = computed(
  () =>
    !!trimmedRoleName.value &&
    !roleNameError.value &&
    !!schemaId.value &&
    !loadingTables.value &&
    !saving.value &&
    (!isEditing.value || hasChanges.value)
);

watch(schemaId, loadTables, { immediate: true });

async function loadTables(newSchemaId: string | null) {
  tableNames.value = [];
  originalPermissions.value = {};
  permissions.value = {};
  errorMessage.value = "";

  if (!newSchemaId) {
    return;
  }

  loadingTables.value = true;
  try {
    const names = await getRootTableNames(newSchemaId);
    if (schemaId.value === newSchemaId) {
      tableNames.value = names;
      originalPermissions.value = Object.fromEntries(
        names.map((tableName) => [
          tableName,
          isEditing.value
            ? storedPermission(tableName)
            : noPermissions(tableName),
        ])
      );
      permissions.value = Object.fromEntries(
        names.map((tableName) => [tableName, storedPermission(tableName)])
      );
    }
  } catch (error) {
    if (schemaId.value === newSchemaId) {
      errorMessage.value = errorToMessage(
        error,
        `Error loading the tables of schema '${newSchemaId}'`
      );
    }
  } finally {
    if (schemaId.value === newSchemaId) {
      loadingTables.value = false;
    }
  }
}

function initialRoleName(): string {
  if (!props.role || props.mode === "create") {
    return "";
  } else {
    return props.mode === "copy" ? `${props.role.role}_copy` : props.role.role;
  }
}

function storedPermission(tableName: string): TablePermission {
  const stored = props.role
    ? adminSettings
        .getPermissionsForCustomRole(props.role)
        .find((permission) => permission.table === tableName)
    : undefined;

  if (!stored) {
    return noPermissions(tableName);
  } else {
    return {
      table: tableName,
      select: Boolean(stored.select),
      insert: Boolean(stored.insert),
      update: Boolean(stored.update),
      delete: Boolean(stored.delete),
      isRowLevel: Boolean(stored.isRowLevel),
    };
  }
}

function effectivePermission(permission: TablePermission): TablePermission {
  return hasSomeRights(permission)
    ? { ...permission }
    : noPermissions(permission.table);
}

function isSamePermission(a: TablePermission, b: TablePermission): boolean {
  return (
    RIGHTS.every((right) => a[right] === b[right]) &&
    a.isRowLevel === b.isRowLevel
  );
}

function noPermissions(tableName: string): TablePermission {
  return {
    table: tableName,
    select: false,
    insert: false,
    update: false,
    delete: false,
    isRowLevel: false,
  };
}

function hasAllRights(permission: TablePermission): boolean {
  return RIGHTS.every((right) => permission[right]);
}

function hasSomeRights(permission: TablePermission): boolean {
  return RIGHTS.some((right) => permission[right]);
}

function setRights(permission: TablePermission, value: boolean) {
  RIGHTS.forEach((right) => {
    permission[right] = value;
  });
}

function setRightsForAllTables(value: boolean) {
  tablePermissions.value.forEach((permission) => setRights(permission, value));
}

async function save() {
  if (!canSave.value || !schemaId.value) {
    return;
  }
  saving.value = true;
  errorMessage.value = "";
  try {
    if (isEditing.value) {
      await adminSettings.editCustomRole({
        schemaId: schemaId.value,
        role: trimmedRoleName.value,
        permissions: changedPermissions.value,
        addedUsers: addedUsers.value,
        removedUsers: removedUsers.value,
      });
    } else {
      await adminSettings.addCustomRole({
        schemaId: schemaId.value,
        role: trimmedRoleName.value,
        permissions: changedPermissions.value,
        users: Array.from(selectedUsers.value),
      });
    }
    visible.value = false;
  } catch (error) {
    errorMessage.value = errorToMessage(
      error,
      isEditing.value ? "Error saving role" : "Error creating role"
    );
  } finally {
    saving.value = false;
  }
}

type Right = (typeof RIGHTS)[number];
</script>

<template>
  <Modal v-model:visible="visible" max-width="max-w-9/10">
    <template #header>
      <header
        class="py-5 px-8 min-h-[4.75rem] flex-none border-b border-divider flex justify-between"
      >
        <h2
          id="modal-title"
          class="uppercase text-heading-4xl font-display text-title-contrast"
        >
          {{ title }}
        </h2>
        <button
          @click="visible = false"
          aria-label="Close modal"
          class="top-4 right-4 p-1 absolute"
        >
          <BaseIcon class="text-button-input-toggle" name="cross" />
        </button>
      </header>
    </template>

    <div class="h-full overflow-y-auto p-12.5 bp-0">
      <FormError
        v-if="errorMessage"
        id="role-modal-error"
        class="mb-8"
        :message="errorMessage"
        :show-prev-next-buttons="false"
      />

      <Field
        id="role-modal-name"
        class="pb-8"
        type="STRING"
        label="Role name"
        v-model="roleName"
        placeholder="Enter a role name"
        :disabled="isEditing"
        :required="!isEditing"
        :invalid="!!roleNameError"
        :errorMessage="roleNameError"
      />

      <Field
        id="role-modal-schema"
        class="pb-8"
        type="ENUM"
        label="Schema"
        v-model="schemaId"
        :allowedValues="schemaIds"
        :disabled="isEditing"
        :required="!isEditing"
      />

      <div class="pb-8 flex flex-col gap-2">
        <span class="text-title-contrast font-bold">Permissions</span>
        <p v-if="!schemaId">No schema selected</p>
        <p v-else-if="loadingTables" class="text-title-contrast">
          Loading tables...
        </p>
        <p v-else-if="!tableNames.length" class="text-title-contrast">
          No tables found in schema '{{ schemaId }}'
        </p>

        <template v-else>
          <Table>
            <template #head>
              <TableHeadRow>
                <TableHead class="w-0">
                  <InputCheckbox
                    id="role-modal-all-permissions"
                    aria-label="Select all permissions for all tables"
                    :modelValue="allTablesHaveAllRights"
                    :indeterminate="
                      !allTablesHaveAllRights && someTableHasRights
                    "
                    @update:modelValue="
                      (checked) => setRightsForAllTables(Boolean(checked))
                    "
                  />
                </TableHead>
                <TableHead class="text-table-column-header">Table</TableHead>
                <TableHead
                  v-for="right in RIGHTS"
                  :key="right"
                  class="text-table-column-header"
                >
                  {{ RIGHT_LABELS[right] }}
                </TableHead>
                <TableHead class="text-table-column-header">
                  Row level
                </TableHead>
              </TableHeadRow>
            </template>
            <template #body>
              <TableRow v-for="tableName in tableNames" :key="tableName">
                <TableCell>
                  <InputCheckbox
                    :id="`role-modal-${tableName}-all`"
                    :aria-label="`Select all permissions for ${tableName}`"
                    :modelValue="hasAllRights(permissions[tableName]!)"
                    :indeterminate="
                      !hasAllRights(permissions[tableName]!) &&
                      hasSomeRights(permissions[tableName]!)
                    "
                    @update:modelValue="
                      (checked) =>
                        setRights(permissions[tableName]!, Boolean(checked))
                    "
                  />
                </TableCell>
                <TableCell>{{ tableName }}</TableCell>
                <TableCell v-for="right in RIGHTS" :key="right">
                  <InputCheckbox
                    :id="`role-modal-${tableName}-${right}`"
                    :aria-label="`${RIGHT_LABELS[right]} ${tableName}`"
                    v-model="permissions[tableName]![right]"
                  />
                </TableCell>
                <TableCell>
                  <InputCheckbox
                    :id="`role-modal-${tableName}-row-level`"
                    :aria-label="`Row level ${tableName}`"
                    :disabled="!hasSomeRights(permissions[tableName]!)"
                    v-model="permissions[tableName]!.isRowLevel"
                  />
                </TableCell>
              </TableRow>
            </template>
          </Table>
          <p class="text-title-contrast">
            Row level limits the permissions to the rows assigned to this role.
            It adds an mg_roles column to the table.
          </p>
        </template>
      </div>

      <Field
        id="role-modal-users"
        class="pb-8"
        type="ENUM_ARRAY"
        :label="`Users (${selectedUsers.length} selected)`"
        v-model="selectedUsers"
        :allowedValues="matchingUserEmails"
      />
      <Message
        v-if="overwrittenRoles.length"
        id="role-modal-overwritten-roles"
        class="gap-3"
      >
        <BaseIcon name="info" class="min-h-6 min-w-6 self-start" />
        <div class="flex flex-col gap-2">
          <p>
            {{
              overwrittenRoles.length === 1
                ? "This user already has"
                : "These users already have"
            }}
            a custom role in schema '{{ schemaId }}'. Saving replaces it with
            this role.
          </p>
          <ul class="list-disc pl-5 font-normal">
            <li v-for="{ email, role } in overwrittenRoles" :key="email">
              {{ email }} (currently {{ role }})
            </li>
          </ul>
        </div>
      </Message>
    </div>

    <template #footer>
      <div class="flex w-full justify-end items-center flex-none">
        <menu class="flex items-center justify-end h-modal-footer">
          <div class="flex gap-4">
            <Button
              id="role-modal-cancel"
              type="secondary"
              :disabled="saving"
              @click="visible = false"
            >
              Cancel
            </Button>
            <Button
              id="role-modal-save"
              type="primary"
              :disabled="!canSave"
              @click="save"
            >
              Save
              <BaseIcon
                v-if="saving"
                class="inline animate-spin"
                name="ProgressActivity"
              />
            </Button>
          </div>
        </menu>
      </div>
    </template>
  </Modal>
</template>
