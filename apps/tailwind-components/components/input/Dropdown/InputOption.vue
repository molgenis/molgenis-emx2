<script lang="ts" setup>
import { ref, computed, watch } from "vue";
import { InputLabel, InputCheckboxIcon, InputRadioIcon } from "#components";
import { type IInputProps } from "../../../types/types";
import type {
  IInputValueLabel,
  recordValue,
} from "../../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<
    IInputProps & {
      label: string;
      option: recordValue;
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

const optionElemId = computed<string>(() => {
  return props.id.replace(/\\s+/g, "").toLowerCase();
});

const emit = defineEmits(["select", "deselect"]);

function toggleSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  if (target.checked) {
    emit("select", props.label);
  } else {
    emit("deselect", props.label);
  }
}
</script>

<template>
  <div class="border p-4">
    <div class="grid grid-cols-[2fr_40px] justify-start items-center">
      <InputLabel
        :for="`${id}-ref-dropdown-option-${optionElemId}-input`"
        class="group flex justify-start items-center relative gap-4"
        :class="{
          'text-disabled cursor-not-allowed': disabled,
          'text-title-contrast cursor-pointer ': !disabled,
        }"
      >
        <input
          :id="`${id}-ref-dropdown-option-${optionElemId}-input`"
          :type="multiselect ? 'checkbox' : 'radio'"
          :name="`${id}-ref-dropdown`"
          :value="label"
          :disabled="disabled"
          :checked="checked"
          class="sr-only"
          @change="toggleSelect"
        />
        <InputCheckboxIcon v-if="multiselect" :checked="checked" />
        <InputRadioIcon v-else :checked="checked" />
        <span class="block text-title-contrast font-bold">
          {{ label }}
        </span>
      </InputLabel>
      <button
        :id="`${id}-ref-dropdown-option-${optionElemId}-toggle`"
        class="w-[60px]"
        :aria-controls="`${id}-ref-dropdown-option-${optionElemId}-content`"
        :aria-expanded="isExpanded"
        :aria-haspopup="true"
        @click="isExpanded = !isExpanded"
      >
        <span class="sr-only"> {{ label }} information </span>
        <BaseIcon name="caret-down" />
      </button>
    </div>
    <div
      :id="`${id}-ref-dropdown-option-${optionElemId}-content`"
      :aria-labelledby="`${id}-ref-dropdown-option-${optionElemId}-toggle`"
      class="px-4"
      :class="{
        hidden: disabled || !isExpanded,
      }"
    >
      <div class="pl-[30px]">
        <slot></slot>
      </div>
    </div>
  </div>
</template>
