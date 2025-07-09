<script lang="ts" setup>
import { ref } from "vue";
import { type IInputProps } from "../../types/types";

import InputDropdownToggle from "./Dropdown/Toggle.vue";
import InputDropdownContainer from "./Dropdown/Container.vue";
import InputDropdownToolbar from "./Dropdown/Toolbar.vue";

const props = withDefaults(
  defineProps<
    IInputProps & {
      options: IInputValue[] | IInputValueLabel[];
      value?: IInputValue | IInputValueLabel;
    }
  >(),
  {
    placeholder: "Select an option",
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
const searchTerm = ref<string>("");

</script>

<template>
  <div :id="id" class="w-full relative">
    <InputDropdownToggle
      :id="id"
      :elemIdControlledByToggle="`${id}-dropdown-content`"
      ref="toggleElemRef"
    >
      <template #dropdownLabel>
        <span class="w-full">
        {{ displayText }}
      </span>
      </template>
    </InputDropdownToggle>
    <InputDropdownContainer
      :id="`${id}-dropdown-content`"
      :aria-expanded="isExpanded"
      :tabindex="isExpanded ? 1 : 0"
      :class="{
        hidden: disabled || !isExpanded,
      }"
    >
      <InputDropdownToolbar
        :id="id"
        @search="(value: string) => searchTerm = value"
      />
      
    </InputDropdownContainer>
  </div>
</template>