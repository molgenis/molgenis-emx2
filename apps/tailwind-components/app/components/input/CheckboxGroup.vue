<script lang="ts" setup>
import { type IInputProps, type IValueLabel } from "../../../types/types";
import type { columnValue } from "../../../../metadata-utils/src/types";
import InputGroupContainer from "../input/InputGroupContainer.vue";
import InputLabel from "../input/Label.vue";
import InputCheckboxIcon from "../input/CheckboxIcon.vue";
import ButtonText from "../button/Text.vue";

withDefaults(
  defineProps<
    IInputProps & {
      options: IValueLabel[];
      showClearButton?: boolean;
      facetCounts?: Map<string, number>;
      countsLoading?: boolean;
    }
  >(),
  {
    showClearButton: false,
    countsLoading: false,
  }
);

const modelValue = defineModel<columnValue[] | undefined | null>();
const emit = defineEmits([
  "update:modelValue",
  "select",
  "deselect",
  "blur",
  "focus",
]);

function toggleSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  if (target.checked) {
    emit("select", target.value);
  } else {
    emit("deselect", target.value);
  }
  emit("focus");
}

function resetModelValue() {
  modelValue.value = [];
}
</script>

<template>
  <InputGroupContainer
    :id="`${id}-checkbox-group`"
    :aria-describedby="describedBy"
    @focus="emit('focus')"
    @blur="emit('blur')"
  >
    <div
      class="flex flex-row min-w-0"
      v-for="option in options"
      :key="option.value"
    >
      <InputLabel
        :for="`${id}-checkbox-group-${option.value}`"
        class="group flex flex-1 justify-start items-center relative min-w-0 overflow-hidden"
        :class="{
          'text-disabled cursor-not-allowed': disabled,
          'text-title-contrast cursor-pointer': !disabled,
        }"
      >
        <input
          type="checkbox"
          :id="`${id}-checkbox-group-${option.value}`"
          :name="id"
          :value="option.value"
          v-model="modelValue"
          :checked="modelValue ? modelValue.includes(option.value) : false"
          :disabled="disabled"
          @change="toggleSelect"
          class="ml-4 mt-2 sr-only"
        />
        <InputCheckboxIcon
          :checked="modelValue ? modelValue.includes(option.value) : false"
          :invalid="invalid"
          :valid="valid"
          :disabled="disabled"
        />
        <span class="flex flex-1 items-baseline min-w-0">
          <span
            class="truncate min-w-0"
            v-tooltip.top="option.label"
            v-if="option.label"
          >
            {{ option.label }}
          </span>
          <span class="truncate min-w-0" v-tooltip.top="option.value" v-else>
            {{ option.value }}
          </span>
          <span v-if="facetCounts" class="shrink-0 ml-0.5">
            ({{ facetCounts.get(option.value as string) ?? 0 }})
          </span>
        </span>
      </InputLabel>
    </div>
    <ButtonText
      v-if="showClearButton"
      type="reset"
      :id="`${id}-checkbox-group-clear`"
      class="mt-2 ml-3"
      :form="`${id}-checkbox-group`"
      @click.prevent="resetModelValue"
      :disabled="disabled || null"
    >
      Clear
    </ButtonText>
  </InputGroupContainer>
</template>
