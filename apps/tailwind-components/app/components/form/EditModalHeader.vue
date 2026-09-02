<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  columnId,
  columnValue,
  IInputValue,
  IInputValueLabel,
} from "../../../../metadata-utils/src/types";
import { useSession } from "../../composables/useSession";
import DraftLabel from "../label/DraftLabel.vue";

const GLOBAL_ROLE: string = "Global";

const { formValues, schemaId } = defineProps<{
  formValues: Record<columnId, columnValue>;
  tableId: string;
  schemaId: string;
  isInsert: boolean;
}>();

const session = useSession(schemaId);

const selectedRole = ref<string>(getSelectedRole());

const roles = computed<string[]>(() => session.rowLevelRoles.value);
const isDraft = computed(() => formValues["mg_draft"] === true || false);
const showRoles = computed(
  () =>
    session.isAdmin.value || session.isOwner.value || session.isManager.value
);

function onUpdateSelectedRole(newRole?: IInputValue | IInputValueLabel | null) {
  selectedRole.value = typeof newRole === "string" ? newRole : "";
  if (selectedRole.value && selectedRole.value !== GLOBAL_ROLE) {
    formValues["mg_roles"] = [selectedRole.value];
  } else {
    formValues["mg_roles"] = [];
  }
}

function getSelectedRole(): string {
  if (
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
  <div class="mb-5 relative flex items-center pr-14">
    <h2 class="uppercase text-heading-4xl font-display text-title-contrast">
      {{ isInsert ? "Add" : "Edit" }} {{ tableId }}
    </h2>

    <DraftLabel v-if="isDraft" />
    <div
      v-if="showRoles"
      class="gap-2.5 ml-auto flex shrink-0 items-center text-title-contrast"
    >
      <label class="whitespace-nowrap font-bold" for="roleSelector">
        Access group
      </label>
      <InputListbox
        id="roleSelector"
        :value="selectedRole"
        :options="[GLOBAL_ROLE].concat(roles)"
        @update:modelValue="onUpdateSelectedRole"
      />
    </div>
  </div>
</template>
