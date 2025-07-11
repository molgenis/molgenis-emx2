<template>
  <div class="w-full relative">
    <InputListboxToggle
      :id="id"
      ref="btnElemRef"
      :aria-controls="`listbox-${id}-options`"
      :invalid="invalid"
      :valid="valid"
      :disabled="disabled"
      :selected-element-id="selectedElementId"
      @keydown="onListboxButtonKeyDown"
    >
      <span class="w-full">
        {{ displayText }}
      </span>
    </InputListboxToggle>
    <div
      role="listbox"
      :id="`listbox-${id}-options`"
      :aria-expanded="isExpanded"
      :tabindex="isExpanded ? 1 : 0"
      class="absolute b-0 w-full z-10 bg-input"
      :class="{
        hidden: disabled || !isExpanded,
      }"
    >
      <label :for="`listbox-${id}-options-search`" class="sr-only">
        Search for items
      </label>
      <InputSearch
        v-if="enableSearch"
        ref="listbox-search"
        :id="`listbox-${id}-options-search`"
        :aria-labelledby="`listbox-${id}-options-search`"
        :aria-controls="`listbox-${id}-options-list`"
        placeholder="Search"
        v-model="searchTerm"
        @update:model-value="emit('search', searchTerm)"
      />
      <ul
        ref="ul"
        :id="`listbox-${id}-options-list`"
        class="overflow-y-scroll z-10 bg-input border"
        :class="{
          'h-44': isExpanded && listboxOptions.length > 5,
          'shadow-inner': !disabled && isExpanded,
        }"
        @keydown.prevent="onListboxKeyDown"
      >
        <InputListboxListItem
          v-for="option in listboxOptions"
          ref="listbox-li"
          :id="option.elemId"
          :isSelected="option.value === '' || isSelected(option.value as IInputValue)"
          :label="(option.label as string) || (option.value as string)"
          @click="onListboxOptionClick(option)"
          @blur="blurListOption(option)"
          @keydown="(event: KeyboardEvent) => onListboxOptionKeyDown(event, option)"
        />
        <li
          v-if="listboxOptions.length === 1 && listboxOptions[0].value === null"
          class="flex justify-center items-center h-[56px] pl-3 py-1 bg-input border-b-[1px] last:border-b-0 text-input italic"
        >
          No options found
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, useTemplateRef, nextTick, watch, onMounted, computed } from "vue";
import type {
  IInputValue,
  IInputValueLabel,
} from "../../../metadata-utils/src/types";
import type {
  IInternalListboxOption,
  IListboxLiRef,
} from "../../types/listbox";

import { InputSearch, InputListboxToggle } from "#components";
import { type IInputProps } from "../../types/types";

const props = withDefaults(
  defineProps<
    IInputProps & {
      options: IInputValue[] | IInputValueLabel[];
      value?: IInputValue | IInputValueLabel;
      enableSearch?: boolean;
    }
  >(),
  {
    placeholder: "Select an option",
    enableSearch: false,
  }
);

const emit = defineEmits([
  "update:modelValue",
  "error",
  "blur",
  "focus",
  "search",
]);

const sourceDataType = ref<string>("");
const sourceData = ref<IInputValueLabel[]>();
const focusCounter = ref<number>(0);
const modelValue = defineModel<IInputValue | IInputValueLabel | null>();
const liElemRefs = useTemplateRef<IListboxLiRef[]>("listbox-li");
const btnElemRef = ref<InstanceType<typeof InputListboxToggle>>();
const searchElemRef = ref<InstanceType<typeof InputSearch>>();
const displayText = ref<string>(props.placeholder);
const startingCounter = ref<number>(0);
const selectedElementId = ref<string>("");
const searchTerm = defineModel<string>("");

const isExpanded = computed<boolean>(() => {
  return btnElemRef.value?.expanded as boolean;
});

onMounted(() => {
  if (props.value && listboxOptions.value) {
    const initalValue: IInternalListboxOption[] = listboxOptions.value.filter(
      (row: IInternalListboxOption) => {
        return (
          row.value === (props.value as IInputValueLabel).value ||
          row.value === (props.value as IInputValue)
        );
      }
    );
    if (initalValue) {
      updateModelValue(initalValue[0], false);
    }
  }
});

watch(
  () => props.placeholder,
  () => (displayText.value = props.placeholder)
);

watch(isExpanded, () => {
  searchTerm.value = "";
  emit("search", searchTerm.value);
});

function counterIsInRange(value: number) {
  return value <= listboxOptions.value.length - 1 && value >= 0;
}

const listboxOptions = computed<IInternalListboxOption[]>(() => {
  const testDataValue: IInputValue | IInputValueLabel = props.options[0];
  if (
    typeof testDataValue === "object" &&
    Object.hasOwn(testDataValue as IInputValueLabel, "value")
  ) {
    sourceDataType.value = "ListboxOptions";
    sourceData.value = props.options as IInputValueLabel[];
  } else {
    sourceDataType.value = "ListboxValue";
    const inputData = props.options as IInputValue[];
    const processedData = inputData.map(
      (value: IInputValue): IInputValueLabel => {
        return { value: value };
      }
    );
    sourceData.value = processedData as IInputValueLabel[];
  }

  const nullOption: IInputValueLabel = {
    value: null,
    label: props.placeholder,
  };

  const inputData = sourceData.value as IInputValueLabel[];
  const data: IInputValueLabel[] = [nullOption, ...inputData];

  return data.map((option: IInputValueLabel, index: number) => {
    return {
      ...option,
      index: index,
      elemId: `listbox-${props.id}-options-${index}`,
    };
  });
});

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
    if (liElemRefs.value) {
      const targetElem = liElemRefs.value[focusCounter.value].li;
      targetElem.setAttribute("tabindex", "0");
      targetElem.focus();
    }
  });
}

function blurListOption(option: IInternalListboxOption) {
  nextTick(() => {
    if (liElemRefs.value) {
      const targetElem = liElemRefs.value[option.index].li;
      targetElem.setAttribute("tabindex", "-1");
      targetElem.blur();
    }
  });
}

function updateDisplayText(text: string | undefined | null): string {
  if (typeof text === "undefined" || text === null || text === "") {
    return props.placeholder;
  }
  return text;
}

function updateModelValue(
  selection: IInternalListboxOption,
  enableToggling: boolean = true
) {
  focusCounter.value = selection.index;
  startingCounter.value = selection.index;

  if (sourceDataType.value === "ListboxOptions") {
    const selectedOption: IInputValueLabel = { value: selection.value };
    if (Object.hasOwn(selection, "label")) {
      selectedOption.label = selection.label;
      displayText.value = updateDisplayText(selectedOption.label);
    } else {
      displayText.value = updateDisplayText(selectedOption.value as string);
    }

    modelValue.value = selectedOption;
  } else {
    modelValue.value = selection.value as IInputValue;
    displayText.value = updateDisplayText(selection.value as string);
  }
  selectedElementId.value = selection.elemId;
  emit("update:modelValue", modelValue.value);

  if (enableToggling) {
    openCloseListbox();
  }
}

function resetModelValue() {
  updateCounter(0);
  displayText.value = updateDisplayText("");
  modelValue.value = undefined;
  emit("update:modelValue", modelValue.value);

  if (isExpanded.value) {
    openCloseListbox();
  }
}

function isSelected(value: IInputValue): boolean {
  if (modelValue.value) {
    return (
      value === (modelValue.value as IInputValueLabel).value ||
      value === (modelValue.value as IInputValue)
    );
  } else {
    return value === null;
  }
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
  btnElemRef.value?.button?.focus();
}

function focusListBoxSearch() {
  searchElemRef.value?.search?.focus();
}

function openCloseListbox() {
  btnElemRef.value!.expanded = !btnElemRef.value!.expanded;
  if (isExpanded.value) {
    if (modelValue.value) {
      updateCounter(startingCounter.value);
    } else {
      updateCounter(0);
    }
    focusListOption();
  } else {
    focusListboxButton();
  }
}

function onListboxButtonKeyDown(event: KeyboardEvent) {
  const key = event.key;
  switch (key) {
    case "ArrowUp":
      openCloseListbox();
      focusPreviousOption();
      break;

    case "ArrowDown":
      openCloseListbox();
      break;

    case "Enter":
      openCloseListbox();
      break;

    case "Space":
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

  if (event.shiftKey && key === "Tab") {
    focusListBoxSearch();
  } else if (key === "ArrowUp") {
    focusPreviousOption();
  } else if (key === "ArrowDown") {
    focusNextOption();
  } else if (key === "PageUp") {
    focusPreviousOption(10);
  } else if (key === "PageDown") {
    focusNextOption(10);
  } else if (key === "Home") {
    focusCounter.value = 0;
    focusListOption();
  } else if (key === "End") {
    focusCounter.value = listboxOptions.value.length - 1;
    focusListOption();
  }
}

function onListboxOptionKeyDown(
  event: KeyboardEvent,
  option: IInternalListboxOption
) {
  const key: string = event.key;

  if (event.shiftKey && key === "Tab") {
    focusListBoxSearch();
  } else if (key === "Enter") {
    updateModelValue(option);
  } else if (["Enter", "Spacebar", " ", "Tab"].includes(key)) {
    updateModelValue(option);
  } else if (key === "Escape") {
    blurListOption(option);
    openCloseListbox();
  }
}

function onListboxOptionClick(option: IInternalListboxOption) {
  updateModelValue(option);
}
</script>
