<script lang="ts" setup>
import { ref, computed } from "vue";
import { InputLabel, InputCheckboxIcon, InputRadioIcon } from "#components";
import { type IInputProps } from "../../../types/types";
import type { recordValue } from "../../../../metadata-utils/src/types";

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
  <div class="p-5 max-h-54 overflow-y-scroll bg-input">
    <div class="grid grid-cols-[2fr_42.5px] justify-start items-center">
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
        <div class="-mt-1">
          <InputCheckboxIcon v-if="multiselect" :checked="checked" />
          <InputRadioIcon v-else :checked="checked" />
        </div>
        <span class="block text-input font-bold">
          {{ label }}
        </span>
      </InputLabel>
      <button
        :id="`${id}-ref-dropdown-option-${optionElemId}-toggle`"
        :aria-controls="`${id}-ref-dropdown-option-${optionElemId}-content`"
        :aria-expanded="isExpanded"
        :aria-haspopup="true"
        @click="isExpanded = !isExpanded"
        class="text-input"
      >
        <span class="sr-only"> {{ label }} information </span>
        <BaseIcon
          :width="18"
          name="caret-down"
          class="m-auto transition-all duration-default origin-center"
          :class="{
            'rotate-180': isExpanded,
          }"
        />
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
