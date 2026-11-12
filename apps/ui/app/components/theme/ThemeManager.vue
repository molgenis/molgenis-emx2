<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import Message from "../../../../tailwind-components/app/components/Message.vue";
import Modal from "../../../../tailwind-components/app/components/Modal.vue";
import {
  BUILT_IN_THEMES,
  createThemeId,
  type ICustomTheme,
} from "../../../../tailwind-components/app/utils/themes";
import {
  themeErrorMessage,
  useThemeSettings,
} from "../../composables/useThemeSettings";
import ThemeEditor from "./ThemeEditor.vue";

interface IThemeRow {
  id: string;
  name: string;
  basedOn?: string;
  variables: Record<string, string>;
  editable: boolean;
}

interface IThemeSection {
  title: string;
  rows: IThemeRow[];
}

const props = defineProps<{
  schema?: string;
}>();

const route = useRoute();
const {
  own,
  inherited,
  load,
  applySavedTheme,
  setActiveTheme,
  saveCustomTheme,
  deleteCustomTheme,
} = useThemeSettings(props.schema);

await load(true);

const editing = ref<ICustomTheme | null>(null);
const deleting = ref<IThemeRow | null>(null);
const showDeleteModal = ref(false);
const errorMessage = ref("");

function toRow(theme: ICustomTheme, editable: boolean): IThemeRow {
  return { ...theme, editable };
}

const sections = computed<IThemeSection[]>(() => {
  const builtIn: IThemeSection = {
    title: "Built-in themes",
    rows: BUILT_IN_THEMES.map(({ id, label }) => ({
      id,
      name: label,
      variables: {},
      editable: false,
    })),
  };
  const shared: IThemeSection = {
    title: "Application themes",
    rows: inherited.value.customThemes.map((theme) => toRow(theme, false)),
  };
  const custom: IThemeSection = {
    title: "Custom themes",
    rows: own.value.customThemes.map((theme) => toRow(theme, true)),
  };
  return [builtIn, shared, custom].filter(
    (section) => section.rows.length > 0 || section === custom
  );
});

const takenIds = computed(() =>
  [...own.value.customThemes, ...inherited.value.customThemes].map(
    (theme) => theme.id
  )
);

const otherNames = computed(() =>
  own.value.customThemes
    .filter((theme) => theme.id !== editing.value?.id)
    .map((theme) => theme.name)
);

const clearLabel = computed(() =>
  props.schema ? "Inherit application theme" : "Let visitors choose"
);

async function run(action: () => Promise<void>): Promise<boolean> {
  errorMessage.value = "";
  try {
    await action();
    applySavedTheme(route.query.theme);
    return true;
  } catch (error) {
    errorMessage.value = themeErrorMessage(error);
    return false;
  }
}

function duplicate(row: IThemeRow) {
  errorMessage.value = "";
  editing.value = {
    id: "",
    name: "",
    basedOn: row.basedOn ?? row.id,
    variables: { ...row.variables },
  };
}

function edit(row: IThemeRow) {
  errorMessage.value = "";
  editing.value = {
    id: row.id,
    name: row.name,
    basedOn: row.basedOn as string,
    variables: { ...row.variables },
  };
}

function closeEditor() {
  editing.value = null;
  errorMessage.value = "";
  applySavedTheme(route.query.theme);
}

async function saveTheme(theme: ICustomTheme) {
  const saved = await run(() =>
    saveCustomTheme({
      ...theme,
      id: theme.id || createThemeId(theme.name, takenIds.value),
    })
  );
  if (saved) {
    editing.value = null;
  }
}

function confirmDelete(row: IThemeRow) {
  deleting.value = row;
  showDeleteModal.value = true;
}

async function removeTheme() {
  const id = deleting.value?.id;
  showDeleteModal.value = false;
  if (id) {
    await run(() => deleteCustomTheme(id));
  }
}
</script>

<template>
  <ThemeEditor
    v-if="editing"
    :theme="editing"
    :other-names="otherNames"
    :error-message="errorMessage"
    @save="saveTheme"
    @cancel="closeEditor"
  />
  <div v-else class="flex flex-col gap-8">
    <Message v-if="errorMessage" id="theme-manager-error" :invalid="true">
      {{ errorMessage }}
    </Message>

    <div>
      <Button
        type="outline"
        size="small"
        :disabled="!own.active"
        @click="run(() => setActiveTheme(undefined))"
      >
        {{ clearLabel }}
      </Button>
    </div>

    <section v-for="section in sections" :key="section.title">
      <h3 class="mb-3 text-heading-xl font-display">{{ section.title }}</h3>
      <p v-if="section.rows.length === 0" class="text-record-value">
        No custom themes yet. Duplicate a theme to create one.
      </p>
      <ul class="flex flex-col divide-y divide-gray-200">
        <li
          v-for="row in section.rows"
          :key="row.id"
          class="flex flex-wrap items-center justify-between gap-3 py-3"
        >
          <div>
            <span class="font-bold">{{ row.name }}</span>
            <span v-if="row.basedOn" class="ml-2 text-sm">
              based on {{ row.basedOn }}
            </span>
            <span v-if="own.active === row.id" class="ml-2 text-sm font-bold">
              (active)
            </span>
          </div>
          <div class="flex flex-wrap gap-2">
            <Button
              size="small"
              type="secondary"
              :disabled="own.active === row.id"
              @click="run(() => setActiveTheme(row.id))"
            >
              Use
            </Button>
            <Button size="small" type="outline" @click="duplicate(row)">
              Duplicate and customise
            </Button>
            <template v-if="row.editable">
              <Button size="small" type="outline" @click="edit(row)">
                Edit
              </Button>
              <Button
                size="small"
                type="outline"
                :disabled="own.active === row.id"
                @click="confirmDelete(row)"
              >
                Delete
              </Button>
            </template>
          </div>
        </li>
      </ul>
    </section>

    <Modal
      v-model:visible="showDeleteModal"
      :title="`Delete theme: ${deleting?.name}`"
    >
      <p class="p-5">This action cannot be undone.</p>
      <template #footer>
        <div class="flex gap-1 m-1">
          <Button size="small" @click="removeTheme">Delete</Button>
          <Button size="small" type="outline" @click="showDeleteModal = false">
            Cancel
          </Button>
        </div>
      </template>
    </Modal>
  </div>
</template>
