<template>
  <div class="w-full relative">
    <InputListboxToggle
      :id="id"
      ref="btnElemRef"
      :aria-controls="`listbox-${id}-options`"
      :required="required"
      :disabled="disabled"
      :aria-labelledby="labelId"
      :selected-element-id="selectedElementId"
      @keydown="onListboxButtonKeyDown"
    >
      <span class="w-full">
        {{ displayText }}
      </span>
    </InputListboxToggle>
    <InputListboxList
      :id="`listbox-${id}-options`"
      :isExpanded="isExpanded"
      :hasFixedHeight="listboxOptions.length > 5"
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
    </InputListboxList>
  </div>
</template>

<script lang="ts" setup>
import { ref, useTemplateRef, nextTick, watch, onMounted } from "vue";
import type {
  columnValue,
  IFieldError,
  IInputValue,
  IInputValueLabel,
} from "../../../metadata-utils/src/types";
import type {
  IInternalListboxOption,
  IListboxLiRef,
} from "../../types/listbox";

import { InputListboxToggle } from "#components";

const props = withDefaults(
  defineProps<{
    id: string;
    labelId: string;
    options: IInputValue[] | IInputValueLabel[];
    value?: IInputValue | IInputValueLabel;
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
  (e: "update:modelValue", value: IInputValue | IInputValueLabel): void;
  (e: "error", value: IFieldError[]): void;
  (e: "blur", value: null): void;
  (e: "focus", value: null): void;
}>();

defineExpose({
  validate,
});

const sourceDataType = ref<string>("");
const sourceData = ref<IInputValueLabel[]>();
const focusCounter = ref<number>(0);
const modelValue = defineModel<IInputValue | IInputValueLabel | null>();
const liElemRefs = useTemplateRef<IListboxLiRef[]>("listbox-li");
const btnElemRef = ref<InstanceType<typeof InputListboxToggle>>();
const displayText = ref<string>(props.placeholder);
const startingCounter = ref<number>(0);
const selectedElementId = ref<string>("");

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

  const defaultOption: IInputValueLabel = {
    value: null,
    label: props.placeholder,
  };
  const inputData = sourceData.value as IInputValueLabel[];
  const data: IInputValueLabel[] = [defaultOption, ...inputData];

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
  console.log(text);
  if (typeof text === "undefined" || text === null) {
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

function onListboxOptionKeyDown(
  event: KeyboardEvent,
  option: IInternalListboxOption
) {
  const key: string = event.key;
  switch (key) {
    case "Enter":
      updateModelValue(option);
      break;

    case "Spacebar":
      updateModelValue(option);
      break;

    // spacebar (for older browser support)
    case " ":
      updateModelValue(option);
      break;

    case "Tab":
      updateModelValue(option);
      break;

    case "Escape":
      blurListOption(option);
      openCloseListbox();
      break;
  }
}

function onListboxOptionClick(option: IInternalListboxOption) {
  updateModelValue(option);
}

function validate(value: columnValue) {
  if (props.required && value === "") {
    emit("error", [{ message: `${props.id} required to complete the form` }]);
  } else {
    emit("error", []);
  }
}
</script>
