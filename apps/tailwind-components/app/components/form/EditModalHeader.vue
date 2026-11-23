<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  columnId,
  columnValue,
  IInputValue,
  IInputValueLabel,
} from "../../../../metadata-utils/src/types";
import { useSession } from "../../composables/useSession";

const GLOBAL_ROLE: string = "Global";

const { formValues, tableId, schemaId } = defineProps<{
  formValues: Record<columnId, columnValue>;
  tableId: string;
  schemaId: string;
  isInsert: boolean;
}>();

const session = await useSession(schemaId);

const roles = computed<string[]>(() => session.rowLevelRoles.value);
const showRoles = computed(() => session.showRolesForTable(tableId));
const selectedRole = ref<string>(getSelectedRole());

function onUpdateSelectedRole(newRole?: IInputValue | IInputValueLabel | null) {
  selectedRole.value = typeof newRole === "string" ? newRole : "";
  if (selectedRole.value === GLOBAL_ROLE) {
    formValues["mg_roles"] = [];
  } else if (selectedRole.value) {
    formValues["mg_roles"] = [selectedRole.value];
  } else {
    delete formValues["mg_roles"];
  }
}

function getSelectedRole(): string {
  if (!showRoles.value) {
    return "";
  } else if (
    Array.isArray(formValues.mg_roles) &&
    typeof formValues.mg_roles[0] === "string"
  ) {
    return formValues.mg_roles[0];
  } else {
    return GLOBAL_ROLE;
  }
}
</script>

<template>
  <div v-if="showRoles" class="h-[7rem] flex-col items-center justify-start">
    <label class="text-title-contrast" for="roleSelector"> Access group </label>
    <InputListbox
      id="roleSelector"
      :value="selectedRole"
      :options="[GLOBAL_ROLE].concat(roles)"
      showClearButton
      @update:modelValue="onUpdateSelectedRole"
    />
  </div>
</template>
