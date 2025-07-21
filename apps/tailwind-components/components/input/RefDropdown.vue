<script lang="ts" setup>
import { ref, computed, watch, onBeforeMount } from "vue";

import { InputSearch, Button, InputListbox } from "#components";
import InputDropdownOption from "./dropdown/InputOption.vue";
import InputDropdownToggle from "./dropdown/Toggle.vue";
import InputDropdownContainer from "./dropdown/Container.vue";
import InputDropdownToolbar from "./dropdown/Toolbar.vue";

import { fetchTableMetadata } from "#imports";

import type { IInputProps, IValueLabel } from "../../types/types";
import type {
  ITableMetaData,
  columnValueObject,
  recordValue,
} from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<
    IInputProps & {
      refSchemaId: string;
      refTableId: string;
      refLabel: string;
      multiselect?: boolean;
      limit?: number;
    }
  >(),
  {
    placeholder: "Select an option",
    multiselect: false,
    limit: 10,
  }
);

const emit = defineEmits([
  "update:modelValue",
  "error",
  "blur",
  "focus",
  "search",
]);

const isExpanded = ref<boolean>(false);
const toggleElemRef = ref<InstanceType<typeof InputDropdownToggle>>();
const displayText = ref<string>(props.placeholder);
const searchTerm = defineModel<string>("");
const sortMethod = ref<string>("Ascending");

const modelValue = defineModel<columnValueObject[] | columnValueObject>();
const tableMetadata = ref<ITableMetaData>();

function updateDisplayText(text: string | undefined | null): string {
  if (typeof text === "undefined" || text === null || text === "") {
    return props.placeholder;
  }
  return text;
}

async function setTableMetadata() {
  tableMetadata.value = await fetchTableMetadata(
    props.refSchemaId,
    props.refTableId
  );
}

onBeforeMount(() => setTableMetadata());

watch([props.placeholder], () => {
  displayText.value = updateDisplayText("");
});
</script>

<template>
  <InputGroupContainer
    :id="`${id}-ref-dropdown`"
    class="w-full relative"
    @focus="emit('focus')"
    @blur="emit('blur')"
  >
    <InputDropdownToggle
      :id="id"
      :elemIdControlledByToggle="`${id}-ref-dropdown-content`"
      ref="toggleElemRef"
      @click="isExpanded = !isExpanded"
    >
      <template #ref-dropdown-label>
        <span class="w-full">
          {{ displayText }}
        </span>
      </template>
    </InputDropdownToggle>
    <InputDropdownContainer
      :id="`${id}-ref-dropdown-content`"
      :aria-expanded="isExpanded"
      :tabindex="isExpanded ? 1 : 0"
      class="border rounded"
      :class="{
        hidden: disabled || !isExpanded,
      }"
    >
      <!-- for background -->
      <InputDropdownToolbar class="">
        <div class="w-full">
          <label :for="`${id}-ref-dropdown-search`" class="sr-only">
            search for values
          </label>
          <InputSearch
            :id="`${id}ref-dropdown-search`"
            v-model="searchTerm"
            placeholder="Search"
          />
        </div>
        <div class="w-[175px]">
          <label
            :id="`${id}-ref-dropdown-sort-input-label`"
            :for="`${id}-ref-dropdown-sort-input`"
            class="sr-only"
          >
            sort data by
          </label>
          <InputListbox
            :id="`${id}-ref-dropdown-sorting`"
            :labelId="`${id}-ref-dropdown-sort-input-label`"
            :options="['Ascending', 'Descending']"
            @update:model-value="(value: string) => (sortMethod = value)"
            :enable-search="false"
            placeholder="Sort data by"
          />
        </div>
      </InputDropdownToolbar>
    </InputDropdownContainer>
    <fieldset :id="`${id}-ref-dropdown-options`">
      <label></label>
      <!-- need to implement :checked on input option component -->
      <div v-for="option in options">
        <InputDropdownOption
          :id="(option.value as string)"
          :option="(option as IValueLabel)"
          :multiselect="multiselect"
        />
      </div>
    </fieldset>
  </InputGroupContainer>
</template>
