<script setup lang="ts">
import { computed, ref, watch } from "vue";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import Message from "../../../../tailwind-components/app/components/Message.vue";
import InputLabel from "../../../../tailwind-components/app/components/input/Label.vue";
import InputSelect from "../../../../tailwind-components/app/components/input/Select.vue";
import InputString from "../../../../tailwind-components/app/components/input/String.vue";
import { useConfiguredTheme } from "../../../../tailwind-components/app/composables/useConfiguredTheme";
import { loadThemeVariables } from "../../../../tailwind-components/app/utils/themeSources";
import {
  BUILT_IN_THEMES,
  HIDDEN_THEME_VARIABLES,
  diffThemeVariables,
  isSafeThemeValue,
  sanitizeVariables,
  themeVariableGroup,
  toHexColor,
  validateThemeName,
  type ICustomTheme,
} from "../../../../tailwind-components/app/utils/themes";

const props = defineProps<{
  theme: ICustomTheme;
  otherNames: string[];
  errorMessage: string;
}>();

const emit = defineEmits<{
  save: [theme: ICustomTheme];
  cancel: [];
}>();

const GROUP_ORDER = [
  "Palette",
  "Text",
  "Background",
  "Border",
  "Shape",
  "Other",
];

const configuredTheme = useConfiguredTheme();

const name = ref(props.theme.name);
const basedOn = ref(props.theme.basedOn);
const variables = ref<Record<string, string>>({ ...props.theme.variables });
const baseVariables = ref<Record<string, string>>({});
const search = ref("");
const changedOnly = ref(false);
const nameTouched = ref(false);

const builtInIds = BUILT_IN_THEMES.map((theme) => theme.id);

watch(
  basedOn,
  async (themeId) => {
    const loaded = await loadThemeVariables(themeId);
    if (themeId === basedOn.value) {
      baseVariables.value = loaded;
    }
  },
  { immediate: true }
);

watch(
  [basedOn, variables],
  () => {
    configuredTheme.value = {
      id: props.theme.id,
      base: basedOn.value,
      overrides: sanitizeVariables(variables.value),
    };
  },
  { immediate: true, deep: true }
);

const allVariables = computed(() => ({
  ...baseVariables.value,
  ...variables.value,
}));

const nameError = computed(() =>
  validateThemeName(name.value, props.otherNames)
);

const invalidVariables = computed(() =>
  Object.entries(variables.value)
    .filter(([, value]) => value.trim() !== "" && !isSafeThemeValue(value))
    .map(([variable]) => variable)
);

function valueOf(variable: string): string {
  return variables.value[variable] ?? baseVariables.value[variable] ?? "";
}

function isChanged(variable: string): boolean {
  return variables.value[variable] !== undefined;
}

function setValue(variable: string, value: string | number | null | undefined) {
  const text = String(value ?? "");
  if (text === baseVariables.value[variable]) {
    delete variables.value[variable];
  } else {
    variables.value[variable] = text;
  }
}

const groups = computed(() => {
  const term = search.value.trim().toLowerCase();
  const visible = Object.keys(allVariables.value)
    .filter((variable) => !HIDDEN_THEME_VARIABLES.includes(variable))
    .filter((variable) => !term || variable.toLowerCase().includes(term))
    .filter((variable) => !changedOnly.value || isChanged(variable))
    .sort();
  return GROUP_ORDER.map((title) => ({
    title,
    variables: visible.filter(
      (variable) => themeVariableGroup(variable) === title
    ),
  })).filter((group) => group.variables.length > 0);
});

function save() {
  nameTouched.value = true;
  if (nameError.value || invalidVariables.value.length > 0) {
    return;
  }
  emit("save", {
    id: props.theme.id,
    name: name.value.trim(),
    basedOn: basedOn.value,
    variables: diffThemeVariables(variables.value, baseVariables.value),
  });
}
</script>

<template>
  <form class="flex flex-col gap-6" @submit.prevent="save">
    <h3 class="text-heading-xl font-display">
      {{ theme.id ? `Edit ${theme.name}` : "New custom theme" }}
    </h3>

    <div class="grid gap-4 md:grid-cols-2">
      <div>
        <InputLabel for="theme-name">Name</InputLabel>
        <InputString
          id="theme-name"
          v-model="name"
          :invalid="nameTouched && !!nameError"
          describedBy="theme-name-error"
          @blur="nameTouched = true"
        />
        <p
          v-if="nameTouched && nameError"
          id="theme-name-error"
          class="mt-1 text-invalid"
        >
          {{ nameError }}
        </p>
      </div>
      <div>
        <InputLabel for="theme-based-on">Based on</InputLabel>
        <InputSelect
          id="theme-based-on"
          v-model="basedOn"
          :options="builtInIds"
        />
      </div>
    </div>

    <div class="flex flex-wrap items-center gap-4">
      <div class="grow">
        <InputLabel for="theme-search">Search variables</InputLabel>
        <InputString id="theme-search" v-model="search" />
      </div>
      <label class="flex items-center gap-2">
        <input v-model="changedOnly" type="checkbox" />
        Changed only
      </label>
    </div>

    <section v-for="group in groups" :key="group.title">
      <h4 class="mb-3 text-heading-lg font-display">{{ group.title }}</h4>
      <div
        v-for="variable in group.variables"
        :key="variable"
        class="grid items-center gap-2 py-1 md:grid-cols-[1fr_3rem_2fr_5rem]"
      >
        <InputLabel :for="`theme-variable-${variable}`" class="break-all">
          {{ variable }}
        </InputLabel>
        <input
          v-if="toHexColor(valueOf(variable), allVariables)"
          type="color"
          class="w-10 h-10 cursor-pointer"
          :aria-label="`Pick colour for ${variable}`"
          :value="toHexColor(valueOf(variable), allVariables)"
          @input="setValue(variable, ($event.target as HTMLInputElement).value)"
        />
        <span v-else />
        <InputString
          :id="`theme-variable-${variable}`"
          :model-value="valueOf(variable)"
          :invalid="
            valueOf(variable).trim() !== '' &&
            !isSafeThemeValue(valueOf(variable))
          "
          @update:model-value="setValue(variable, $event)"
        />
        <Button
          v-if="isChanged(variable)"
          size="tiny"
          type="outline"
          :aria-label="`Reset ${variable}`"
          @click.prevent="delete variables[variable]"
        >
          Reset
        </Button>
      </div>
    </section>

    <Message
      v-if="invalidVariables.length > 0"
      id="theme-variables-error"
      :invalid="true"
    >
      These values are not allowed: {{ invalidVariables.join(", ") }}. Values
      cannot contain url(), @, ;, braces, &lt; or backslashes.
    </Message>
    <Message v-if="errorMessage" id="theme-editor-error" :invalid="true">
      {{ errorMessage }}
    </Message>

    <div class="sticky bottom-0 flex gap-2 py-3 bg-content">
      <Button size="small" @click.prevent="save">Save theme</Button>
      <Button size="small" type="outline" @click.prevent="emit('cancel')">
        Cancel
      </Button>
    </div>
  </form>
</template>
