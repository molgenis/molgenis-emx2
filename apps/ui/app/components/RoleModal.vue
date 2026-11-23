<template>
  <Modal v-model:visible="visible" :title="title" max-width="max-w-9/10">
    <div class="overflow-y-auto p-5 flex flex-col gap-6">
      <FormError
        v-if="errorMessage"
        id="role-modal-error"
        :message="errorMessage"
        :show-prev-next-buttons="false"
      />

      <div class="flex flex-col gap-2">
        <label for="role-modal-name" class="text-title-contrast font-bold">
          Role name
        </label>
        <InputString
          id="role-modal-name"
          v-model="roleName"
          placeholder="Enter a role name"
          :disabled="isEditing"
          :invalid="!!roleNameError"
          :describedBy="roleNameError ? 'role-modal-name-error' : undefined"
        />
        <p v-if="roleNameError" id="role-modal-name-error" class="text-invalid">
          {{ roleNameError }}
        </p>
      </div>

      <div class="flex flex-col gap-2">
        <label for="role-modal-schema" class="text-title-contrast font-bold">
          Schema
        </label>
        <InputEnum
          id="role-modal-schema"
          v-model="schemaId"
          :allowedValues="schemaIds"
          :disabled="isEditing"
          required
        />
      </div>

      <div v-if="schemaId" class="flex flex-col gap-2">
        <span class="text-title-contrast font-bold">Permissions</span>
        <p v-if="loadingTables" class="text-title-contrast">
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
                <TableHead class="text-table-column-header"
                  >Row level</TableHead
                >
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

      <div class="flex flex-col gap-2">
        <span class="text-title-contrast font-bold">
          Users ({{ selectedUsers.size }} selected)
        </span>
        <InputSearch
          id="role-modal-user-search"
          v-model="userSearch"
          placeholder="Search users"
          size="small"
        />
        <ul class="max-h-64 overflow-y-auto">
          <li
            v-for="email in matchingUserEmails"
            :key="email"
            class="flex items-center gap-2 py-1 text-title-contrast"
          >
            <InputCheckbox
              :id="`role-modal-user-${email}`"
              :aria-label="`Assign ${email} to role`"
              :modelValue="selectedUsers.has(email)"
              @update:modelValue="
                (checked) => setUserSelected(email, Boolean(checked))
              "
            />
            <label :for="`role-modal-user-${email}`" class="cursor-pointer">
              {{ email }}
            </label>
            <span v-if="userNote(email)" class="italic">
              {{ userNote(email) }}
            </span>
          </li>
          <li v-if="!matchingUserEmails.length" class="text-title-contrast">
            No users found
          </li>
        </ul>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-between items-center">
        <menu class="flex items-center justify-end h-modal-footer">
          <div class="flex gap-4">
            <Button
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

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import BaseIcon from "../../../tailwind-components/app/components/BaseIcon.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import FormError from "../../../tailwind-components/app/components/form/Error.vue";
import InputEnum from "../../../tailwind-components/app/components/input/Enum.vue";
import InputCheckbox from "../../../tailwind-components/app/components/input/Checkbox.vue";
import InputSearch from "../../../tailwind-components/app/components/input/Search.vue";
import InputString from "../../../tailwind-components/app/components/input/String.vue";
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import Table from "../../../tailwind-components/app/components/Table.vue";
import TableCell from "../../../tailwind-components/app/components/TableCell.vue";
import TableHead from "../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../tailwind-components/app/components/TableRow.vue";
import { errorToMessage } from "../../../tailwind-components/app/utils/errorToMessage";
import useAdminSettings from "../composables/useAdminSettings.ts";
import type { CustomRole, RolePermission } from "../interfaces/interfaces.ts";
import { getRootTableNames } from "../util/adminUtils";

const RIGHTS = ["select", "insert", "update", "delete"] as const;
type Right = (typeof RIGHTS)[number];
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

const props = defineProps<{
  role?: CustomRole;
}>();

const visible = defineModel("visible", {
  required: true,
});

const adminSettings = await useAdminSettings();

const isEditing = computed(() => !!props.role);
const title = computed(() =>
  props.role ? `Edit role: ${props.role.role}` : "Create role"
);
const originalUsers = new Set(props.role?.users ?? []);

const roleName = ref(props.role?.role ?? "");
const schemaId = ref<string | null>(props.role?.schemaId ?? null);
const tableNames = ref<string[]>([]);
const originalPermissions = ref<Record<string, RolePermission>>({});
const permissions = ref<Record<string, RolePermission>>({});
const loadingTables = ref(false);
const userSearch = ref("");
const selectedUsers = ref<Set<string>>(new Set(originalUsers));
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
      customRole.role !== props.role?.role
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
  otherCustomRolesInSchema.value.reduce((rolesByUser, customRole) => {
    customRole.users.forEach((email) => {
      rolesByUser[email] = customRole.role;
    });
    return rolesByUser;
  }, {} as Record<string, string>)
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
    .filter((permission): permission is RolePermission => !!permission)
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
  Array.from(originalUsers).filter((email) => !selectedUsers.value.has(email))
);

const hasChanges = computed(
  () =>
    changedPermissions.value.length > 0 ||
    addedUsers.value.length > 0 ||
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
        names.map((tableName) => [tableName, storedPermission(tableName)])
      );
      permissions.value = Object.fromEntries(
        names.map((tableName) => [
          tableName,
          { ...originalPermissions.value[tableName]! },
        ])
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

function storedPermission(tableName: string): RolePermission {
  const stored = props.role
    ? adminSettings
        .getPermissionsForCustomRole(props.role)
        .find((permission) => permission.table === tableName)
    : undefined;
  if (!stored) {
    return noPermissions(tableName);
  }
  return {
    table: tableName,
    select: Boolean(stored.select),
    insert: Boolean(stored.insert),
    update: Boolean(stored.update),
    delete: Boolean(stored.delete),
    isRowLevel: Boolean(stored.isRowLevel),
  };
}

function effectivePermission(permission: RolePermission): RolePermission {
  return hasSomeRights(permission)
    ? { ...permission }
    : noPermissions(permission.table);
}

function isSamePermission(a: RolePermission, b: RolePermission): boolean {
  return (
    RIGHTS.every((right) => a[right] === b[right]) &&
    a.isRowLevel === b.isRowLevel
  );
}

function noPermissions(tableName: string): RolePermission {
  return {
    table: tableName,
    select: false,
    insert: false,
    update: false,
    delete: false,
    isRowLevel: false,
  };
}

function hasAllRights(permission: RolePermission): boolean {
  return RIGHTS.every((right) => permission[right]);
}

function hasSomeRights(permission: RolePermission): boolean {
  return RIGHTS.some((right) => permission[right]);
}

function setRights(permission: RolePermission, value: boolean) {
  RIGHTS.forEach((right) => {
    permission[right] = value;
  });
}

function setRightsForAllTables(value: boolean) {
  tablePermissions.value.forEach((permission) => setRights(permission, value));
}

function userNote(email: string): string {
  const otherRole = customRoleByUser.value[email];
  const selected = selectedUsers.value.has(email);
  if (isEditing.value && originalUsers.has(email) && !selected) {
    return "will be removed from this role";
  }
  if (otherRole) {
    return selected && !originalUsers.has(email)
      ? `will be removed from role '${otherRole}'`
      : `member of role '${otherRole}'`;
  }
  return "";
}

function setUserSelected(email: string, selected: boolean) {
  if (selected) {
    selectedUsers.value.add(email);
  } else {
    selectedUsers.value.delete(email);
  }
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
</script>
