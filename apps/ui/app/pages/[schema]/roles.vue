<template>
  <Container>
    <PageHeader :title="`Roles in ${schemaLabel}`" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
    </PageHeader>

    <ContentBlock v-if="!isManager" class="mt-3">
      <p>You need Manager or Owner access to manage roles.</p>
    </ContentBlock>

    <ContentBlock v-else-if="loading" class="mt-3">
      <p>Loading...</p>
    </ContentBlock>

    <ContentBlock v-else-if="errorMessage" class="mt-3">
      <p class="text-red-600">{{ errorMessage }}</p>
    </ContentBlock>

    <ContentBlock v-else class="mt-3" title="Permission Matrix">
      <div
        v-if="successMessage"
        class="mb-3 p-2 bg-green-100 text-green-800 rounded text-sm"
      >
        {{ successMessage }}
      </div>

      <RoleEditor
        v-if="roles.length"
        :roles="roles"
        :selectedRole="selectedRoleName"
        @select="selectedRoleName = $event"
        @create="handleCreateRole"
        @delete="handleDeleteRole"
      />

      <PermissionMatrix
        v-if="selectedRole"
        :role="selectedRole"
        :tables="tables"
        :readonly="selectedRole.system"
        @save="handleSave"
        @dropPermission="handleDropPermission"
      />

      <p v-if="!roles.length" class="text-sm text-gray-500">
        No roles defined yet. Create a new role to get started.
      </p>
    </ContentBlock>
  </Container>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { useRoute, useRouter } from "#app/composables/router";
import { useHead } from "#app";
import { useSession } from "../../../../tailwind-components/app/composables/useSession";
import {
  getRolesAndTables,
  saveRole,
  createRole,
  deleteRole,
  dropPermission,
  type IRoleInfo,
  type ITableInfo,
  type IPermission,
} from "~/util/roleUtils";
import type { Crumb } from "../../../../tailwind-components/types/types";
import Container from "../../../../tailwind-components/app/components/Container.vue";
import PageHeader from "../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../tailwind-components/app/components/BreadCrumbs.vue";
import ContentBlock from "../../../../tailwind-components/app/components/content/ContentBlock.vue";
import RoleEditor from "~/components/RoleEditor.vue";
import PermissionMatrix from "~/components/PermissionMatrix.vue";

const route = useRoute();
const router = useRouter();
const schema = route.params.schema as string;

useHead({ title: `Roles - ${schema} - Molgenis` });

const { session, isAdmin } = await useSession();

const isManager = computed(() => {
  if (isAdmin.value) return true;
  const userRoles = session.value?.roles || [];
  return userRoles.includes("Manager") || userRoles.includes("Owner");
});

const roles = ref<IRoleInfo[]>([]);
const tables = ref<ITableInfo[]>([]);
const selectedRoleName = ref((route.query.role as string) || "");
const loading = ref(true);
const errorMessage = ref("");
const successMessage = ref("");
const schemaLabel = ref(schema);

const selectedRole = computed(
  () => roles.value.find((role) => role.name === selectedRoleName.value) || null
);

watch(selectedRoleName, (name) => {
  router.replace({ query: name ? { role: name } : {} });
});

const crumbs: Crumb[] = [
  { label: schema, url: `/${schema}` },
  { label: "roles", url: "" },
];

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await getRolesAndTables(schema);
    roles.value = data.roles;
    tables.value = data.tables;
    const matchesExisting = data.roles.some(
      (role) => role.name === selectedRoleName.value
    );
    if ((!selectedRoleName.value || !matchesExisting) && data.roles.length) {
      const firstCustom = data.roles.find((role) => !role.system);
      selectedRoleName.value = firstCustom?.name || data.roles[0]!.name;
    }
  } catch (err: any) {
    errorMessage.value = err?.message || "Failed to load roles";
  } finally {
    loading.value = false;
  }
}

async function handleSave(permissions: IPermission[]) {
  successMessage.value = "";
  errorMessage.value = "";
  try {
    await saveRole(
      schema,
      selectedRoleName.value,
      selectedRole.value?.description ?? null,
      permissions
    );
    successMessage.value = "Permissions saved successfully";
    await loadData();
    setTimeout(() => {
      successMessage.value = "";
    }, 3000);
  } catch (err: any) {
    errorMessage.value = err?.message || "Failed to save permissions";
  }
}

async function handleCreateRole(name: string, description: string | null) {
  successMessage.value = "";
  errorMessage.value = "";
  try {
    await createRole(schema, name, description);
    selectedRoleName.value = name;
    successMessage.value = `Role "${name}" created`;
    await loadData();
    setTimeout(() => {
      successMessage.value = "";
    }, 3000);
  } catch (err: any) {
    errorMessage.value = err?.message || "Failed to create role";
  }
}

async function handleDeleteRole(roleName: string) {
  successMessage.value = "";
  errorMessage.value = "";
  try {
    await deleteRole(schema, roleName);
    selectedRoleName.value = "";
    successMessage.value = `Role "${roleName}" deleted`;
    await loadData();
    setTimeout(() => {
      successMessage.value = "";
    }, 3000);
  } catch (err: any) {
    errorMessage.value = err?.message || "Failed to delete role";
  }
}

async function handleDropPermission(roleName: string, tableName: string) {
  successMessage.value = "";
  errorMessage.value = "";
  try {
    await dropPermission(schema, roleName, tableName);
    successMessage.value = `Permission for "${tableName}" cleared`;
    await loadData();
    setTimeout(() => {
      successMessage.value = "";
    }, 3000);
  } catch (err: any) {
    errorMessage.value = err?.message || "Failed to clear permission";
  }
}

await loadData();
</script>
