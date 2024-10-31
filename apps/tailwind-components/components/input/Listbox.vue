<template>
  <ul>
    <li>selection: {{ selectedElemId }}:{{ modelValue }}</li>
    <li>hover: {{ hoveredOption }}</li>
    <li>focus: {{ focusedOption }}</li>
  </ul>
  <div class="w-full relative">
    <span :id="`listbox-${id}-label`" class="block mb-3">
      {{ listboxLabel }}
    </span>
    <button
      :id="`listbox-${id}-toggle`"
      class="relative h-10 w-full text-left pl-10 bg-listbox border-input text-listbox"
      :aria-controls="`listbox-${id}-options`"
      @click="onToggleClick"
    >
      <span class="truncate" v-if="modelValue?.label">
        {{ modelValue.label }}
      </span>
      <span v-else-if="modelValue?.value">
        {{ modelValue.value }}
      </span>
      <span class="truncate" v-else>
        {{ placeholder }}
      </span>
      <span
        class="absolute right-5 top-2"
        :class="{ 'rotate-180': listboxIsExpanded }"
        aria-hidden="true"
      >
        <BaseIcon name="caret-down" />
      </span>
    </button>
    <ul
      role="listbox"
      :id="`listbox-${id}-options`"
      :aria-expanded="listboxIsExpanded"
      :aria-activedescendant="selectedElemId"
      tabindex="0"
      class="absolute b-0 w-full z-10 bg-listbox"
      :class="{
        'hidden shadow-search-input': !listboxIsExpanded,
      }"
    >
      <li
        v-for="(option, index) in listboxOptions"
        :id="`listbox-${id}-options-${index}`"
        :key="index"
        role="option"
        :ref="listboxOptionRefs.set"
        :aria-selected="option.value === modelValue?.value"
        :data-value="option.value"
        :data-label="option.label"
        class="flex justify-start items-center gap-3 pl-3 py-1 text-listbox border-t-[1px] border-t-listbox-option cursor-pointer focus:bg-listbox-hover focus:text-listbox hover:bg-listbox-hover hover:text-listbox"
        :class="{
          'bg-listbox-selected text-listbox-selected':
            option.value === modelValue?.value,
        }"
        @click="(event: Event) => onListboxSelection(option, (event.target as HTMLInputElement).id)"
        @mouseover="onListboxHover(option)"
        @mouseout="hoveredOption = null"
        @keypress="(event: KeyboardEvent) => onKeyboardEvent(event.key, option)"
      >
        <span
          aria-hidden="true"
          :class="{
            visible: option.value === modelValue?.value,
          }"
        >
          <BaseIcon name="Check" class="text-listbox-selected" :width="16" />
        </span>
        <span v-if="(option as IListboxOption).label">
          {{ (option as IListboxOption).label }}
        </span>
        <span v-else>
          {{ option.value }}
        </span>
      </li>
    </ul>
  </div>
</template>

<script lang="ts" setup>
import { useTemplateRefsList } from "@vueuse/core";
import { ref } from "vue";

interface IListboxOption {
  value: string | number | boolean;
  label?: string;
}

withDefaults(
  defineProps<{
    id: string;
    listboxLabel: string;
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
const selectedElemId = ref<string>("");
const hoveredOption = ref<IListboxOption | null>();
const focusedOption = ref<IListboxOption | null>();
const listboxIsExpanded = ref<boolean>(false);

const listboxOptionRefs = useTemplateRefsList<HTMLLIElement>();

function onToggleClick() {
  listboxIsExpanded.value = !listboxIsExpanded.value;
  if (listboxIsExpanded.value) {
    listboxOptionRefs.value[0].focus();
    focusedOption.value = {
      value: listboxOptionRefs.value[0].getAttribute("data-value") as string,
      label: listboxOptionRefs.value[0].getAttribute("data-label") as string,
    };
  }
}

function onListboxSelection(option: IListboxOption, elemId: string) {
  modelValue.value = option;
  selectedElemId.value = elemId;
  listboxIsExpanded.value = false;
}

function onListboxHover(option: IListboxOption) {
  hoveredOption.value = option;
  focusedOption.value = null;
}

function onListboxFocus(option: IListboxOption) {
  focusedOption.value = option;
}

function onKeyboardEvent(key: string, option: IListboxOption | null) {
  console.log(key);
}
</script>
