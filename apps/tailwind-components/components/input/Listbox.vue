<template>
  <output class="block w-full mb-6 bg-gray-100 py-2 px-2">
    {{ focusCounter }}: {{ modelValue }}
  </output>

  <div class="w-full relative">
    <button
      ref="listboxToggle"
      :id="`listbox-${id}-toggle`"
      role="combobox"
      aria-haspopup="listbox"
      :aria-controls="`listbox-${id}-options`"
      :aria-required="required"
      :aria-expanded="listboxIsExpanded"
      class="flex justify-start items-center h-10 w-full text-left pl-10 border-input bg-input text-button-input-toggle"
      @click="openCloseListbox"
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
      ref="listboxListRef"
      :aria-expanded="listboxIsExpanded"
      class="absolute b-0 w-full z-10 bg-listbox"
      :class="{ hidden: !listboxIsExpanded }"
      tabindex="0"
      @keydown.space.prevent
      @keydown.up.prevent="onKeyUp"
      @keydown.down.prevent="onKeyDown"
    >
      <li
        v-for="option in listboxOptions"
        ref="listboxOptionsRef"
        :id="`listbox-${id}-options-${option.index}`"
        role="option"
        class="flex justify-start items-center gap-3 pl-3 py-1 text-listbox border-t-[1px] border-t-listbox-option hover:cursor-pointer hover:bg-listbox-hover hover:text-listbox focus:bg-listbox-selected focus:text-listbox-selected"
        :class="{
          '!bg-listbox-selected !text-listbox-selected':
            option.value === modelValue?.value,
        }"
        :aria-selected="option.value === modelValue?.value"
        @click="(event: Event) => updateModelValue(event, option)"
        @focus="(event: Event) => updateModelValue(event, option)"
        @blur="(event: Event) => (event.target as HTMLOptionElement).setAttribute('tabindex', '-1')"
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
}

const props = withDefaults(
  defineProps<{
    id: string;
    labelId: string;
    options: IListboxOption[];
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

const focusCounter = ref<number>(0);
const listboxIsExpanded = ref<boolean>(false);
const modelValue = defineModel<IInternalListboxOption>();
const ulElemRef = useTemplateRef<HTMLUListElement>("listboxListRef");
const olElemRefs = useTemplateRef<HTMLOptionElement[]>("listboxOptionsRef");
const btnElemRef = useTemplateRef<HTMLButtonElement>("listboxToggle");

const listboxOptions = computed<IInternalListboxOption[]>(() => {
  const defaultOption: IListboxOption = {
    value: null,
    label: props.placeholder,
  };
  const data: IListboxOption[] = [defaultOption, ...props.options];
  return data.map((option: IListboxOption, index: number) => {
    return { ...option, index: index };
  });
});

function counterIsInRange(value: number) {
  return value <= listboxOptions.value.length - 1 && value >= 0;
}

function updateCounter(value: number) {
  if (counterIsInRange(value)) {
    focusCounter.value = value;
  } else {
    focusCounter.value = focusCounter.value;
  }
}

function focusListOption() {
  nextTick(() => {
    if (olElemRefs.value) {
      const targetElem = olElemRefs.value[
        focusCounter.value
      ] as HTMLOptionElement;
      targetElem.setAttribute("tabindex", "0");
      targetElem.focus();
    }
  });
}

function updateModelValue(event: Event, selection: IInternalListboxOption) {
  const elemId: string = (event.target as HTMLOptionElement).id;
  btnElemRef.value?.setAttribute("aria-activedescendant", elemId);
  modelValue.value = selection;
  focusCounter.value = selection.index;
}

function onKeyUp(event: Event) {
  const newCounterValue = focusCounter.value - 1;
  updateCounter(newCounterValue);
  focusListOption();
}

function onKeyDown(event: Event) {
  const newCounterValue = focusCounter.value + 1;
  updateCounter(newCounterValue);
  focusListOption();
}

function openCloseListbox() {
  listboxIsExpanded.value = !listboxIsExpanded.value;
  if (listboxIsExpanded.value && modelValue.value) {
    updateCounter(modelValue.value.index);
  } else {
    updateCounter(0);
  }
  focusListOption();
}
</script>
