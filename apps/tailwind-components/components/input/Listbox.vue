<template>
  <output class="block w-full mb-6 bg-gray-100 py-2 px-2">
    <code>{{ focusCounter }}: {{ modelValue }}</code>
  </output>

  <div class="w-full relative">
    <button
      :id="id"
      ref="listbox-button"
      role="combobox"
      :disabled="disabled"
      aria-haspopup="listbox"
      :aria-controls="`listbox-${id}-options`"
      :aria-required="required"
      :aria-expanded="listboxIsExpanded"
      :aria-labelledby="labelId"
      class="flex justify-start items-center h-10 w-full text-left pl-10 border-input bg-input text-button-input-toggle focus:ring-blue-300"
      @click="openCloseListbox"
      @keydown="onListboxButtonKeyDown"
    >
      <span class="w-full" v-if="modelValue?.label">
        {{ modelValue.label }}
      </span>
      <span class="w-full" v-else-if="modelValue?.value">
        {{ modelValue.value }}
      </span>
      <span class="w-full" v-else>
        {{ placeholder }}
      </span>
      <div class="w-[60px] flex flex-col">
        <BaseIcon :width="18" name="caret-up" class="mx-auto -my-1" />
        <BaseIcon :width="18" name="caret-down" class="mx-auto -my-1" />
      </div>
    </button>

    <ul
      :id="`listbox-${id}-options`"
      role="listbox"
      ref="listbox-ul"
      :aria-expanded="listboxIsExpanded"
      class="absolute b-0 w-full overflow-y-scroll z-10 bg-listbox"
      :class="{
        hidden: !listboxIsExpanded,
        'h-44': listboxIsExpanded && listboxOptions.length > 5,
        'shadow-inner': listboxIsExpanded,
      }"
      @keydown="onListboxKeyDown"
    >
      <li
        v-for="option in listboxOptions"
        ref="listbox-li"
        :id="option.elemId"
        role="option"
        class="flex justify-start items-center gap-3 pl-3 py-1 text-listbox border-t-[1px] border-t-listbox-option hover:cursor-pointer hover:bg-listbox-hover hover:text-listbox focus:bg-listbox-hover focus:text-listbox"
        :class="{
          '!bg-listbox-selected !text-listbox-selected':
            option.value === modelValue?.value,
        }"
        :aria-selected="option.value === modelValue?.value"
        @click="onListboxOptionClick(option)"
        @blur="blurListOption(option)"
        @keydown="(event: KeyboardEvent) => onListboxOptionKeyDown(event, option)"
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
    </ul>
  </div>
</template>

<script lang="ts" setup>
import { ref, useTemplateRef, nextTick } from "vue";

interface IListboxOption {
  value: string | number | boolean | undefined | null;
  label?: string;
}

interface IInternalListboxOption extends IListboxOption {
  index: number;
  elemId: string;
}

const props = withDefaults(
  defineProps<{
    id: string;
    labelId: string;
    options: IListboxOption[];
    required?: boolean;
    hasError?: boolean;
    placeholder?: string;
    disabled?: boolean;
  }>(),
  {
    disabled: false,
    required: false,
    hasError: false,
    placeholder: "Select an option",
  }
);

const focusCounter = ref<number>(0);
const listboxIsExpanded = ref<boolean>(false);
const modelValue = defineModel<IInternalListboxOption>();
const ulElemRef = useTemplateRef<HTMLUListElement>("listbox-ul");
const olElemRefs = useTemplateRef<HTMLOptionElement[]>("listbox-li");
const btnElemRef = useTemplateRef<HTMLButtonElement>("listbox-button");

const listboxOptions = computed<IInternalListboxOption[]>(() => {
  const defaultOption: IListboxOption = {
    value: null,
    label: props.placeholder,
  };
  const data: IListboxOption[] = [defaultOption, ...props.options];
  return data.map((option: IListboxOption, index: number) => {
    return {
      ...option,
      index: index,
      elemId: `listbox-${props.id}-options-${index}`,
    };
  });
});

function counterIsInRange(value: number) {
  return value <= listboxOptions.value.length - 1 && value >= 0;
}

function updateCounter(value: number) {
  if (counterIsInRange(value)) {
    focusCounter.value = value;
  } else {
    if (value > listboxOptions.value.length - 1) {
      focusCounter.value = listboxOptions.value.length - 1;
    } else {
      focusCounter.value = 0;
    }
  }
}

function focusListOption() {
  nextTick(() => {
    if (olElemRefs.value) {
      const targetElem = olElemRefs.value[focusCounter.value];
      targetElem.setAttribute("tabindex", "0");
      targetElem.focus();
    }
  });
}

function blurListOption(option: IInternalListboxOption) {
  nextTick(() => {
    if (olElemRefs.value) {
      const targetElem = olElemRefs.value[option.index];
      targetElem.setAttribute("tabindex", "-1");
      targetElem.blur();
    }
  });
}

function updateModelValue(selection: IInternalListboxOption) {
  btnElemRef.value?.setAttribute("aria-activedescendant", selection.elemId);
  modelValue.value = selection;
  focusCounter.value = selection.index;
  openCloseListbox();
}

function focusPreviousOption(by: number = 1) {
  const newCounterValue = focusCounter.value - by;
  updateCounter(newCounterValue);
  focusListOption();
}

function focusNextOption(by: number = 1) {
  const newCounterValue = focusCounter.value + by;
  updateCounter(newCounterValue);
  focusListOption();
}

function focusListboxButton() {
  btnElemRef.value?.focus();
}

function openCloseListbox() {
  listboxIsExpanded.value = !listboxIsExpanded.value;
  if (listboxIsExpanded.value) {
    ulElemRef.value?.setAttribute("tabindex", "0");
    if (modelValue.value) {
      updateCounter(modelValue.value.index);
    } else {
      updateCounter(0);
    }
    focusListOption();
  } else {
    ulElemRef.value?.setAttribute("tabindex", "-1");
  }
}

function onListboxButtonKeyDown(event: KeyboardEvent) {
  const key = event.key;
  switch (key) {
    case "ArrowUp":
      event.preventDefault();
      openCloseListbox();
      focusPreviousOption();
      break;

    case "ArrowDown":
      event.preventDefault();
      openCloseListbox();
      break;

    case "Enter":
      event.preventDefault();
      openCloseListbox();
      break;

    case "Space":
      event.preventDefault();
      openCloseListbox();
      break;

    case "Home":
      focusCounter.value = 0;
      focusListOption();
      break;

    case "End":
      focusCounter.value = listboxOptions.value.length - 1;
      focusListOption();
      break;
  }
}

function onListboxKeyDown(event: KeyboardEvent) {
  const key = event.key;
  switch (key) {
    // focus
    case "ArrowUp":
      event.preventDefault();
      focusPreviousOption();
      break;

    case "ArrowDown":
      event.preventDefault();
      focusNextOption();
      break;

    case "PageUp":
      event.preventDefault();
      focusPreviousOption(10);
      break;

    case "PageDown":
      event.preventDefault();
      focusNextOption(10);
      break;

    case "Home":
      focusCounter.value = 0;
      focusListOption();
      break;

    case "End":
      focusCounter.value = listboxOptions.value.length - 1;
      focusListOption();
      break;
  }
}

function onListboxOptionClick(option: IInternalListboxOption) {
  updateModelValue(option);
  focusListboxButton();
}

function onListboxOptionKeyDown(
  event: KeyboardEvent,
  option: IInternalListboxOption
) {
  const key: string = event.key;
  switch (key) {
    case "Enter":
      event.preventDefault();
      updateModelValue(option);
      focusListboxButton();
      break;

    case "Spacebar":
      event.preventDefault();
      updateModelValue(option);
      focusListboxButton();
      break;

    // spacebar (for older browser support)
    case " ":
      event.preventDefault();
      updateModelValue(option);
      focusListboxButton();
      break;

    case "Tab":
      updateModelValue(option);
      focusListboxButton();
      break;

    case "Escape":
      event.preventDefault();
      blurListOption(option);
      openCloseListbox();
      focusListboxButton();
      break;
  }
}
</script>
