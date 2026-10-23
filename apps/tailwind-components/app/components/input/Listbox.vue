<template>
  <InputGroupContainer
    ref="root"
    class="w-full"
    @focus="emit('focus')"
    @blur="emit('blur')"
  >
    <div class="relative">
      <div
        class="flex flex-wrap items-center gap-1 min-h-input border rounded-alt"
        :class="{
          'p-2.5': hasChips,
          'bg-input border-invalid text-invalid focus-within:outline focus-within:outline-2 focus-within:outline-current':
            invalid && !disabled,
          'bg-input border-valid text-valid focus-within:outline focus-within:outline-2 focus-within:outline-current':
            valid && !disabled,
          'bg-disabled border-disabled text-disabled cursor-not-allowed':
            disabled,
          'bg-disabled border-valid text-valid cursor-not-allowed':
            valid && disabled,
          'bg-disabled border-invalid text-invalid cursor-not-allowed':
            invalid && disabled,
          'bg-input text-input hover:border-input-hover focus-within:border-input-focused':
            !disabled && !invalid && !valid,
        }"
      >
        <template v-if="multiple">
          <Button
            v-for="(item, index) in selectedItems"
            :key="`${index}-${labelOf(item)}`"
            type="filterWell"
            size="tiny"
            icon="cross"
            iconPosition="right"
            :label="labelOf(item)"
            :aria-label="`Remove ${labelOf(item)}`"
            :disabled="disabled"
            @click.prevent="removeItem(index)"
          />
        </template>
        <InputListboxToggle
          :id="id"
          ref="toggle"
          :aria-controls="listboxId"
          :aria-describedby="describedBy"
          :disabled="disabled"
          :required="required"
          :expanded="isExpanded"
          @click="isExpanded ? closePanel() : openPanel()"
          @keydown="onToggleKeyDown"
        >
          <span
            class="w-full whitespace-nowrap"
            :class="{ 'sr-only': multiple && selectedItems.length }"
          >
            {{ toggleText }}
          </span>
        </InputListboxToggle>
      </div>
      <div
        :id="`listbox-${id}-panel`"
        :tabindex="isExpanded ? 1 : 0"
        class="absolute top-full w-full z-10 bg-input"
        :class="{
          hidden: disabled || !isExpanded,
        }"
        @keydown="onPanelKeyDown"
      >
        <template v-if="enableSearch">
          <label :for="`listbox-${id}-options-search`" class="sr-only">
            Search for items
          </label>
          <InputSearch
            ref="search"
            :id="`listbox-${id}-options-search`"
            placeholder="Search"
            v-model="searchTerm"
            @update:model-value="emit('search', searchTerm)"
            @keydown="onSearchKeyDown"
          />
        </template>
        <div ref="scroller" class="max-h-56 overflow-y-auto bg-input border">
          <ul
            role="listbox"
            :id="listboxId"
            :aria-multiselectable="multiple ? 'true' : undefined"
            @keydown="onListKeyDown"
          >
            <InputListboxListItem
              v-for="option in listboxOptions"
              ref="listbox-li"
              :key="option.elemId"
              :id="option.elemId"
              :isSelected="isSelected(option)"
              :multiple="multiple"
              :label="option.label"
              @click="choose(option)"
              @blur="blurListOption(option)"
              @keydown="(event: KeyboardEvent) => onOptionKeyDown(event, option)"
            />
          </ul>
          <div
            v-if="!listboxOptions.length"
            class="flex justify-center items-center h-input pl-3 py-1 bg-input"
          >
            <TextNoResultsMessage />
          </div>
          <div ref="sentinel" aria-hidden="true" class="h-px"></div>
        </div>
      </div>
    </div>
    <ButtonText
      v-if="showClearButton && selectedItems.length"
      type="button"
      :id="`${id}-listbox-clear`"
      class="mt-2 ml-3"
      :disabled="disabled"
      @click="clear"
    >
      Clear
    </ButtonText>
  </InputGroupContainer>
</template>

<script
  lang="ts"
  setup
  generic="T extends IInputValue | IInputValueLabel, M extends boolean = false"
>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  useTemplateRef,
  watch,
} from "vue";
import type {
  IInputValue,
  IInputValueLabel,
} from "../../../../metadata-utils/src/types";
import type {
  IInternalListboxOption,
  IListboxLiRef,
} from "../../../types/listbox";
import { type IInputProps } from "../../../types/types";
import { useClickOutside } from "../../composables/useClickOutside";
import Button from "../Button.vue";
import ButtonText from "../button/Text.vue";
import TextNoResultsMessage from "../text/NoResultsMessage.vue";
import InputGroupContainer from "./InputGroupContainer.vue";
import InputListboxListItem from "./listbox/ListItem.vue";
import InputListboxToggle from "./listbox/Toggle.vue";
import InputSearch from "./Search.vue";

type Model = M extends true ? T[] : T | null;
type Option = IInternalListboxOption<T>;

const props = withDefaults(
  defineProps<
    IInputProps & {
      options: T[];
      multiple?: M & boolean;
      value?: T;
      enableSearch?: boolean;
      required?: boolean;
      showClearButton?: boolean;
    }
  >(),
  {
    placeholder: "Select an option",
    enableSearch: false,
    showClearButton: false,
  }
);

const emit = defineEmits(["blur", "focus", "search", "load-more"]);

const modelValue = defineModel<Model>();
const searchTerm = defineModel<string>("search");

const root = useTemplateRef("root");
const toggle =
  useTemplateRef<InstanceType<typeof InputListboxToggle>>("toggle");
const search = useTemplateRef<InstanceType<typeof InputSearch>>("search");
const scroller = useTemplateRef<HTMLDivElement>("scroller");
const sentinel = useTemplateRef<HTMLDivElement>("sentinel");
const liElemRefs = useTemplateRef<IListboxLiRef[]>("listbox-li");

const isExpanded = ref<boolean>(false);
const focusCounter = ref<number>(0);

const listboxId = computed(() => `listbox-${props.id}-options`);

const listboxOptions = computed<Option[]>(() =>
  props.options.map((item, index) => ({
    item,
    value: valueOf(item),
    label: labelOf(item),
    index,
    elemId: `listbox-${props.id}-options-${index}`,
  }))
);

const selectedItems = computed<T[]>(() => toItems(modelValue.value));

const hasChips = computed<boolean>(
  () => !!props.multiple && selectedItems.value.length > 0
);

const toggleText = computed<string>(() => {
  if (props.multiple) {
    return selectedItems.value.length
      ? `${selectedItems.value.length} selected`
      : props.placeholder;
  }
  const [first] = selectedItems.value;
  return first === undefined ? props.placeholder : labelOf(first);
});

useClickOutside(root, () => (isExpanded.value = false));

watch(isExpanded, () => {
  searchTerm.value = "";
  emit("search", searchTerm.value);
});

let observer: IntersectionObserver | undefined;

onMounted(() => {
  seedFromValue();
  if (typeof IntersectionObserver === "undefined" || !sentinel.value) {
    return;
  }
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        emit("load-more");
      }
    },
    { root: scroller.value, rootMargin: "50px" }
  );
  observer.observe(sentinel.value);
});

onBeforeUnmount(() => observer?.disconnect());

watch(
  () => props.options,
  async () => {
    await nextTick();
    if (observer && sentinel.value) {
      observer.unobserve(sentinel.value);
      observer.observe(sentinel.value);
    }
  }
);

function valueOf(item: IInputValue | IInputValueLabel) {
  return typeof item === "object" ? item.value : item;
}

function labelOf(item: IInputValue | IInputValueLabel): string {
  return typeof item === "object"
    ? item.label || String(item.value ?? "")
    : String(item);
}

function toItems(model: T[] | T | null | undefined): T[] {
  if (Array.isArray(model)) {
    return model;
  }
  return model == null ? [] : [model];
}

function toModel(items: T[]): Model {
  return (props.multiple ? items : items[0] ?? null) as Model;
}

function commit(items: T[]) {
  modelValue.value = toModel(items);
}

function seedFromValue() {
  const seed = props.value;
  if (seed === undefined) {
    return;
  }
  const match = listboxOptions.value.find(
    (option) => option.value === valueOf(seed)
  );
  if (match) {
    commit([match.item]);
  }
}

function isSelected(option: Option): boolean {
  return selectedItems.value.some((item) => valueOf(item) === option.value);
}

function choose(option: Option) {
  focusCounter.value = option.index;
  if (!props.multiple) {
    commit([option.item]);
    closePanel();
  } else if (isSelected(option)) {
    commit(
      selectedItems.value.filter((item) => valueOf(item) !== option.value)
    );
  } else {
    commit([...selectedItems.value, option.item]);
  }
}

function removeItem(index: number) {
  commit(selectedItems.value.filter((_, position) => position !== index));
  focusToggle();
}

function clear() {
  commit([]);
  focusToggle();
}

function focusToggle() {
  toggle.value?.button?.focus();
}

function openPanel() {
  isExpanded.value = true;
  focusCounter.value = Math.max(0, listboxOptions.value.findIndex(isSelected));
  focusListOption();
}

function closePanel() {
  isExpanded.value = false;
  focusToggle();
}

function moveFocus(to: number) {
  focusCounter.value = Math.min(
    Math.max(to, 0),
    listboxOptions.value.length - 1
  );
  focusListOption();
}

function focusListOption() {
  nextTick(() => {
    const refItem = liElemRefs.value?.[focusCounter.value];
    if (refItem?.li) {
      refItem.li.setAttribute("tabindex", "0");
      refItem.li.focus();
    }
  });
}

function blurListOption(option: Option) {
  nextTick(() => {
    const refItem = liElemRefs.value?.[option.index];
    if (refItem?.li) {
      refItem.li.setAttribute("tabindex", "-1");
      refItem.li.blur();
    }
  });
}

function onToggleKeyDown(event: KeyboardEvent) {
  if (event.key === "ArrowDown" || event.key === "ArrowUp") {
    event.preventDefault();
    if (!isExpanded.value) {
      openPanel();
    }
  }
}

function onListKeyDown(event: KeyboardEvent) {
  const key = event.key;
  if (key === "ArrowUp") {
    moveFocus(focusCounter.value - 1);
  } else if (key === "ArrowDown") {
    moveFocus(focusCounter.value + 1);
  } else if (key === "PageUp") {
    moveFocus(focusCounter.value - 10);
  } else if (key === "PageDown") {
    moveFocus(focusCounter.value + 10);
  } else if (key === "Home") {
    moveFocus(0);
  } else if (key === "End") {
    moveFocus(listboxOptions.value.length - 1);
  } else {
    return;
  }
  event.preventDefault();
}

function onOptionKeyDown(event: KeyboardEvent, option: Option) {
  const key = event.key;
  if (event.shiftKey && key === "Tab") {
    if (props.enableSearch) {
      event.preventDefault();
      search.value?.search?.focus();
    } else {
      isExpanded.value = false;
    }
  } else if (
    ["Enter", "Spacebar", " "].includes(key) ||
    (key === "Tab" && !props.multiple)
  ) {
    event.preventDefault();
    choose(option);
  }
}

function onSearchKeyDown(event: KeyboardEvent) {
  if (event.key === "ArrowDown" || (event.key === "Tab" && !event.shiftKey)) {
    event.preventDefault();
    event.stopPropagation();
    moveFocus(0);
  }
}

function onPanelKeyDown(event: KeyboardEvent) {
  if (event.key === "Escape" || (event.key === "Tab" && props.multiple)) {
    if (!event.shiftKey) {
      event.preventDefault();
      if (event.key === "Escape") {
        event.stopPropagation();
      }
      closePanel();
    }
  }
}
</script>
