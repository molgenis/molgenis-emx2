<script lang="ts" setup>
import { ref, computed } from "vue";
import { InputLabel, InputCheckboxIcon, InputRadioIcon } from "#components";
import { type IInputProps } from "../../../types/types";
import type { IInputValueLabel } from "../../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<
    IInputProps & {
      option: IInputValueLabel;
      checked?: boolean;
      multiselect?: boolean;
    }
  >(),
  {
    placeholder: "Select an option",
    multiselect: false,
  }
);

const isExpanded = ref<boolean>(false);
const value = defineModel<IInputValueLabel>();

const optionElemId = computed<string>(() => {
  return props.id.replace(/\\s+/g, "").toLowerCase();
});
</script>

<template>
  <div class="flex justify-start items-center gap-4">
    <InputLabel
      :for="`${id}-ref-dropdown-option-${option.value}-input`"
      class="group flex justify-start items-center relative"
      :class="{
        'text-disabled cursor-not-allowed': disabled,
        'text-title-contrast cursor-pointer ': !disabled,
      }"
    >
      <input
        :id="`${id}-ref-dropdown-option-${optionElemId}-input`"
        :type="multiselect ? 'checkbox' : 'radio'"
        :name="`${id}-ref-dropdown`"
        v-model="value"
        :disabled="disabled"
        :checked="checked"
        class="ml-4 mt-2 sr-only"
      />

      <InputCheckboxIcon v-if="multiselect" :checked="checked" />
      <InputRadioIcon v-else :checked="checked" />

      <span class="block">
        {{ option.label || option.value }}
      </span>
    </InputLabel>
    <button
      :id="`${id}-ref-dropdown-option-${optionElemId}-toggle`"
      class="w-[60px]"
      :aria-controls="`${id}-ref-dropdown-option-${optionElemId}-content`"
      :aria-expanded="isExpanded"
      :aria-haspopup="true"
    >
      <span class="sr-only"
        >{{ option.label || option.value }} information</span
      >
      <BaseIcon name="caret-down" />
    </button>
  </div>
  <div
    :id="`${id}-ref-dropdown-option-${optionElemId}-content`"
    :aria-labelledby="`${id}-ref-dropdown-option-${optionElemId}-toggle`"
  ></div>
</template>
