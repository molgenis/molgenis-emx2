<template>
  <div class="w-full relative">
    <button
      :id="id"
      ref="listbox-button"
      role="combobox"
      :disabled="disabled"
      aria-haspopup="listbox"
      :aria-controls="`listbox-${id}-options`"
      :aria-required="required"
      :aria-expanded="isExpanded"
      :aria-labelledby="labelId"
      class="flex justify-start items-center h-10 w-full text-left pl-10 border bg-input rounded-search-input text-button-input-toggle focus:ring-blue-300"
      :class="{
        'border-disabled text-disabled bg-disabled': disabled,
        'border-invalid text-invalid': hasError,
      }"
      @click.prevent="openCloseListbox"
      @keydown.prevent="onListboxButtonKeyDown"
    >
      <span class="w-full">
        {{ displayText }}
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
      :aria-expanded="isExpanded"
      class="absolute b-0 w-full overflow-y-scroll z-10"
      :class="{
        hidden: !isExpanded,
        'h-44': isExpanded && listboxOptions.length > 5,
        'shadow-inner': isExpanded,
      }"
      @keydown.prevent="onListboxKeyDown"
    >
      <li
        v-for="option in listboxOptions"
        ref="listbox-li"
        :id="option.elemId"
        role="option"
        class="flex justify-start items-center gap-3 pl-3 py-1 bg-listbox text-listbox border-t-[1px] border-t-listbox-option hover:cursor-pointer hover:bg-listbox-hover hover:text-listbox focus:bg-listbox-hover focus:text-listbox"
        :class="{
          '!bg-listbox-selected !text-listbox-selected': isSelected(
            option.value
          ),
        }"
        :aria-selected="isSelected(option.value)"
        @click="onListboxOptionClick(option)"
        @blur="blurListOption(option)"
        @keydown="(event: KeyboardEvent) => onListboxOptionKeyDown(event, option)"
      >
        <BaseIcon
          name="Check"
          class="fill-listbox-selected"
          :class="isSelected(option.value) ? 'visible' : 'invisible'"
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
import { ref, useTemplateRef, nextTick, watch, onMounted } from "vue";
import type {
  columnValue,
  IFieldError,
} from "../../../metadata-utils/src/types";
import type {
  IListboxValue,
  IListboxOption,
  IInternalListboxOption,
} from "../../types/listbox";

const props = withDefaults(
  defineProps<{
    id: string;
    labelId: string;
    options: IListboxOption[] | IListboxValue[];
    value?: IListboxOption | IListboxValue;
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

const emit = defineEmits<{
  (e: "update:modelValue", value: IListboxOption | IListboxValue | null): void;
  (e: "error", value: IFieldError[] | null): void;
  (e: "blur", value: null): void;
  (e: "focus", value: null): void;
}>();

defineExpose({
  validate,
});

const sourceDataType = ref<string>("");
const sourceData = ref<IListboxOption[]>();
const focusCounter = ref<number>(0);
const isExpanded = ref<boolean>(false);
const modelValue = defineModel<IListboxOption | IListboxValue | null>();
const ulElemRef = useTemplateRef<HTMLUListElement>("listbox-ul");
const olElemRefs = useTemplateRef<HTMLOptionElement[]>("listbox-li");
const btnElemRef = useTemplateRef<HTMLButtonElement>("listbox-button");
const displayText = ref<string>(props.placeholder);
const startingCounter = ref<number>(0);

onMounted(() => {
  if (props.value && listboxOptions.value) {
    const initalValue: IInternalListboxOption[] = listboxOptions.value.filter(
      (row: IInternalListboxOption) => {
        return (
          row.value === (props.value as IListboxOption).value ||
          row.value === (props.value as IListboxValue)
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
  () => {
    displayText.value = props.placeholder;
  }
);

function counterIsInRange(value: number) {
  return value <= listboxOptions.value.length - 1 && value >= 0;
}

const listboxOptions = computed<IInternalListboxOption[]>(() => {
  const testDataValue: IListboxOption | IListboxValue = props.options[0];
  if (
    typeof testDataValue === "object" &&
    Object.hasOwn(testDataValue as IListboxOption, "value")
  ) {
    sourceDataType.value = "ListboxOptions";
    sourceData.value = props.options as IListboxOption[];
  } else {
    sourceDataType.value = "ListboxValue";
    const inputData = props.options as IListboxValue[];
    const processedData = inputData.map(
      (value: IListboxValue): IListboxOption => {
        return { value: value };
      }
    );
    sourceData.value = processedData as IListboxOption[];
  }

  const defaultOption: IListboxOption = {
    value: null,
    label: props.placeholder,
  };
  const inputData = sourceData.value as IListboxOption[];
  const data: IListboxOption[] = [defaultOption, ...inputData];

  return data.map((option: IListboxOption, index: number) => {
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

function updateDisplayText(text: string | undefined | null): string {
  if (typeof text === "undefined" || text === null) {
    return props.placeholder;
  }
  return text;
}

function updateModelValue(
  selection: IInternalListboxOption,
  enableToggling: boolean = true
) {
  btnElemRef.value?.setAttribute("aria-activedescendant", selection.elemId);
  focusCounter.value = selection.index;
  startingCounter.value = selection.index;

  if (sourceDataType.value === "ListboxOptions") {
    const selectedOption: IListboxOption = { value: selection.value };
    if (Object.hasOwn(selection, "label")) {
      selectedOption.label = selection.label;
      displayText.value = updateDisplayText(selectedOption.label);
    } else {
      displayText.value = updateDisplayText(selectedOption.value as string);
    }

    modelValue.value = selectedOption;
  } else {
    modelValue.value = selection.value as IListboxValue;
    displayText.value = updateDisplayText(selection.value as string);
  }

  emit("update:modelValue", modelValue.value);

  if (enableToggling) {
    openCloseListbox();
  }
}

function isSelected(value: IListboxValue): boolean {
  if (modelValue.value) {
    return (
      value === (modelValue.value as IInternalListboxOption).value ||
      value === (modelValue.value as IListboxValue)
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
  btnElemRef.value?.focus();
}

function openCloseListbox() {
  isExpanded.value = !isExpanded.value;
  if (isExpanded.value) {
    ulElemRef.value?.setAttribute("tabindex", "0");
    if (modelValue.value) {
      updateCounter(startingCounter.value);
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
  switch (key) {
    case "ArrowUp":
      focusPreviousOption();
      break;

    case "ArrowDown":
      focusNextOption();
      break;

    case "PageUp":
      focusPreviousOption(10);
      break;

    case "PageDown":
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
      updateModelValue(option);
      focusListboxButton();
      break;

    case "Spacebar":
      updateModelValue(option);
      focusListboxButton();
      break;

    // spacebar (for older browser support)
    case " ":
      updateModelValue(option);
      focusListboxButton();
      break;

    case "Tab":
      updateModelValue(option);
      focusListboxButton();
      break;

    case "Escape":
      blurListOption(option);
      openCloseListbox();
      focusListboxButton();
      break;
  }
}

function validate(value: columnValue) {
  if (props.required && value === "") {
    emit("error", [{ message: `${props.id} required to complete the form` }]);
  } else {
    emit("error", null);
  }
}
</script>
