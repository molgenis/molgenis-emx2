<script lang="ts" setup>
import { ref } from "vue";

import { InputSearch, Button } from "#components";
import InputDropdownOption from "./dropdown/InputOption.vue";
import InputDropdownToggle from "./dropdown/Toggle.vue";
import InputDropdownContainer from "./dropdown/Container.vue";
import InputDropdownToolbar from "./dropdown/Toolbar.vue";

import { type IInputProps } from "../../types/types";
import type { IInputValueLabel } from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<
    IInputProps & {
      options: IInputValueLabel[];
      value?: IInputValueLabel;
      multiselect?: boolean;
    }
  >(),
  {
    placeholder: "Select an option",
    multiselect: false,
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
const options = ref<IInputValueLabel[]>([]);
const selections = defineModel<IInputValueLabel[]>();

// function: to check if the option is *in* the selections
function optionIsChecked (option: IInputValueLabel) {
  console.log(option)
}

</script>

<template>
  <div :id="`${id}-ref-dropdown`" class="w-full relative">
    <InputDropdownToggle
      :id="id"
      :elemIdControlledByToggle="`${id}-ref-dropdown-content`"
      ref="toggleElemRef"
    >
      <template #dropdownLabel>
        <span class="w-full">
          {{ displayText }}
        </span>
      </template>
    </InputDropdownToggle>
    <InputDropdownContainer
      :id="`${id}-ref-dropdown-content`"
      :aria-expanded="isExpanded"
      :tabindex="isExpanded ? 1 : 0"
      :class="{
        hidden: disabled || !isExpanded,
      }"
    >
      <div class="bg-ref-dropdown-toolbar">
        <label :id="`${id}-ref-dropdown-search`" class="sr-only"
          >search for values</label
        >
        <InputSearch
          :id="`${id}ref-dropdown-search`"
          v-model="searchTerm"
          placeholder="Search"
        />
        <div
          class="flex flex-wrap md:flex-nowrap justify-start items-center gap-4 p-2"
        >
          <Button :id="`${id}-ref-dropdown-reset-button`" type="secondary" />
        </div>
      </div>
    </InputDropdownContainer>
    <fieldset :id="`${id}-ref-dropdown-options`">
      <label></label>
      <!-- need to implement :checked on input option component -->
      <div v-for="option in options">
        <InputDropdownOption 
          :id="(option.value as string)"
          :option="(option as IInputValueLabel)"
        />
      </div>
    </fieldset>
  </div>
</template>
