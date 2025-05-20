<template>
  <ul>
    <li @click="open = !open" :class="option.children ? 'clickable' : ''">
      <span class="toggle-icon">
        {{ option.children ? (open ? "&#9660;" : "&#9650;") : "" }}
      </span>
      <input
        @click.stop
        @change="(event) => selectOption((event.target as HTMLInputElement).checked, option)"
        type="checkbox"
        :ref="`${option.name}-checkbox`"
        class="mr-1"
        :checked="selected"
        :indeterminate.prop="isIndeterminate && !selected"
      />
      <label>
        <span class="code">{{ option.code }}</span> {{ option.label }}
      </label>
    </li>
    <li
      v-if="option.children"
      class="border border-right-0 border-bottom-0 border-top-0 indent"
    >
      <tree-component
        v-if="open"
        :options="option.children"
        :facetIdentifier="facetIdentifier"
        @indeterminate-update="handleChildIndeterminateUpdate"
        :filter="filter"
      />
    </li>
  </ul>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, toRefs, watch } from "vue";
import { IOntologyItem } from "../../../interfaces/interfaces";
import { useFiltersStore } from "../../../stores/filtersStore";

/** need to lazy load because it gets too large quickly. Over 9000! */
const TreeComponent = defineAsyncComponent(() => import("./TreeComponent.vue"));

const filtersStore = useFiltersStore();
const emit = defineEmits(["indeterminate-update"]);

const props = withDefaults(
  defineProps<{
    facetIdentifier: string;
    option: IOntologyItem;
    parentSelected?: boolean;
    filter?: string;
  }>(),
  { filter: "" }
);

const { facetIdentifier, option, filter } = toRefs(props);

const open = ref<boolean>(!!filter.value);
const childIsIndeterminate = ref<boolean>(false);

const selectedFilters = computed<any[]>(() => {
  const filters = filtersStore.getFilterValue(facetIdentifier.value) || [];
  console.log(filters);
  return filters;
});

const selected = computed(() => {
  if (!selectedFilters.value?.length) {
    return false;
  } else if (option.value.children?.length) {
    if (numberOfSelectedChildren.value === option.value.children.length) {
      if (!isSelected(option.value)) {
        selectOption(true, option.value);
      }
      return true;
    } else {
      return false;
    }
  } else {
    return isSelected(option.value);
  }
});

const numberOfSelectedChildren = computed(() => {
  if (option.value.children) {
    return option.value.children.filter((child: IOntologyItem) =>
      isSelected(child)
    ).length;
  } else {
    return 0;
  }
});

const isIndeterminate = computed<boolean>(() => {
  if (!option.value.children) {
    return false;
  } else if (childIsIndeterminate.value) {
    return true;
  } else {
    return (
      numberOfSelectedChildren.value > 0 &&
      numberOfSelectedChildren.value < option.value.children.length
    );
  }
});

watch(isIndeterminate, emitIndeterminateStatus);
watch(filter, (newFilter: string) => (open.value = !!newFilter));

function isSelected(option: IOntologyItem) {
  return selectedFilters.value.some((filter) => filter.name === option.name);
}

function selectOption(checked: boolean, option: IOntologyItem) {
  filtersStore.updateOntologyFilter(facetIdentifier.value, option, checked);
}

function handleChildIndeterminateUpdate(newStatus: boolean) {
  childIsIndeterminate.value = newStatus;
}

function emitIndeterminateStatus() {
  emit("indeterminate-update", isIndeterminate.value);
}
</script>

<style scoped>
li {
  margin-right: 1rem;
  list-style-type: none;
}

.toggle-icon {
  font-size: 0.75rem;
  margin-right: 0.5rem;
}

.code {
  font-weight: 200;
}

.indent {
  margin-left: 0.25rem;
  border-width: 2px !important;
}

.clickable:hover > label {
  cursor: pointer;
  background-color: var(--gray-light);
}
</style>
