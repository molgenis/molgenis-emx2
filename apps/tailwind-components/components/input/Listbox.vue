<template>
  <ul class="mb-6">
    <li>selection: {{ modelValue }}</li>
    <li>focus index: {{ focusIndex }}</li>
  </ul>

  <div class="w-full relative">
    <InputListboxToggle
      :id="`listbox-${id}-toggle`"
      :value="modelValue?.value"
      :label="modelValue?.label"
      :placeholder="placeholder"
      :controls="`listbox-${id}-options`"
      :required="required"
      :hasError="hasError"
      @click="openCloseListbox"
    />

    <ul
      :id="`listbox-${id}-options`"
      role="listbox"
      tabindex="0"
      ref="listboxList"
      :aria-expanded="listboxIsExpanded"
      :aria-activedescendant="modelValue?.elemId"
      class="absolute b-0 w-full z-10 bg-listbox"
      :class="{
        hidden: !listboxIsExpanded,
      }"
      @keydown="onKeyboardEvent"
    >
      <li
        v-for="(option, index) in listboxOptions"
        :ref="listboxOptionRefs.set"
        role="option"
        :id="`listbox-${id}-options-${index}`"
        class="flex justify-start items-center gap-3 pl-3 py-1 text-listbox border-t-[1px] border-t-listbox-option cursor-pointer focus:bg-listbox-hover focus:text-listbox hover:bg-listbox-hover hover:text-listbox"
        :class="{
          '!bg-listbox-selected !text-listbox-selected':
            option.value === modelValue?.value,
        }"
        @click="onListOptionClick(option)"
        @focus="console.log(option.value, ' is focused')"
      >
        <BaseIcon
          name="Check"
          class="fill-listbox-selected"
          :class="option.value === modelValue?.value ? 'visible' : 'invisible'"
          :width="18"
        />
        <span v-if="option.label">
          {{ option.label }}
        </span>
        <span v-else>
          {{ option.value }}
        </span>
      </li>
      <!-- <ListboxListItem
        v-for="(option, index) in listboxOptions"
        :key="index"
        :ref="listboxOptionRefs.set"
        :listbox-id="`listbox-${id}-options`"
        :elem-id="`listbox-${id}-options-${index}`"
        :value="option.value"
        :label="option.label"
        :selected="option.value === modelValue?.value"
        @update:model-value="onSelection"
      /> -->
    </ul>
  </div>
</template>

<script lang="ts" setup>
import { useTemplateRefsList } from "@vueuse/core";
import { ref, useTemplateRef } from "vue";

interface IListboxOption {
  elemId?: string;
  value: string | number | boolean;
  label?: string;
  selected?: boolean;
}

const props = withDefaults(
  defineProps<{
    id: string;
    listboxLabelId: string;
    listboxOptions: IListboxOption[];
    required?: boolean;
    hasError?: boolean;
    placeholder?: string;
  }>(),
  {
    required: false,
    hasError: false,
    placeholder: "Select an option",
  }
);

const modelValue = defineModel<IListboxOption>();
const listboxIsExpanded = ref<boolean>(false);
const focusIndex = ref<number>(0);
const listboxListRef = useTemplateRef<HTMLUListElement>("listboxList");
const listboxOptionRefs = useTemplateRefsList();

function focusFirstElement() {
  (listboxOptionRefs.value[0] as HTMLOptionElement).focus();
}

function openCloseListbox() {
  listboxIsExpanded.value = !listboxIsExpanded.value;
  if (listboxIsExpanded.value) {
    console.log(listboxListRef.value);
    listboxListRef.value?.focus();
    focusFirstElement();
  }
}

function onListOptionClick(data: IListboxOption) {
  modelValue.value = data;
  openCloseListbox();
}

function onKeyboardEvent(event: KeyboardEvent) {
  const key = event.key;
  if (key === "ArrowDown") {
    event.preventDefault();
    const newIndexValue: number = focusIndex.value + 1;
    if (newIndexValue > props.listboxOptions.length - 1) {
      focusIndex.value = props.listboxOptions.length - 1;
    } else {
      focusIndex.value = newIndexValue;
    }
  }
  if (key === "ArrowUp") {
    event.preventDefault();
    console.log("up arrow pressed");
    const newIndexValue: number = focusIndex.value - 1;
    if (newIndexValue < 0) {
      focusIndex.value = 0;
    } else {
      focusIndex.value = newIndexValue;
    }
  }

  if (key === "Enter") {
    console.log("new selection");
  }

  if (key === "Spacebar" || key === "" || key === " ") {
    event.preventDefault();
  }
}
</script>
