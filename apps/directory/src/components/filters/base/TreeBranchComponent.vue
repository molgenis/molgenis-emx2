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
        :parentSelected="selected"
        @indeterminate-update="handleChildIndeterminateUpdate"
        :filter="filter"
      />
    </li>
  </ul>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, toRefs, watch } from "vue";
import { useFiltersStore } from "../../../stores/filtersStore";

/** need to lazy load because it gets too large quickly. Over 9000! */
const TreeComponent = defineAsyncComponent(() => import("./TreeComponent.vue"));

const filtersStore = useFiltersStore();
const emit = defineEmits(["indeterminate-update"]);

const props = withDefaults(
  defineProps<{
    facetIdentifier: string;
    option: Record<string, any>;
    parentSelected?: boolean;
    filter?: string;
  }>(),
  { parentSelected: false, filter: "" }
);

const { facetIdentifier, option, parentSelected, filter } = toRefs(props);

const open = ref<boolean>(!!filter.value);
const childIsIndeterminate = ref<boolean>(false);

const selectedFilters = computed<any[]>(
  filtersStore.getFilterValue(facetIdentifier.value) || []
);

const selected = computed(() => {
  if (numberOfSelectedChildren.value === option.value.children?.length) {
    return true;
  } else if (!selectedFilters.value?.length) {
    return false;
  } else {
    return selectedFilters.value.some(
      (selectedValue: Record<string, any>) =>
        selectedValue.name === option.value.name
    );
  }
});

const numberOfSelectedChildren = computed(() => {
  if (option.value.children) {
    return option.value.children.filter((child: Record<string, any>) =>
      isSelected(child)
    ).length;
  } else {
    return 0;
  }
});

function isSelected(option: Record<string, any>) {
  return selectedFilters.value.some((filter) => filter.name === option.name);
}

function getSelectedChildCount(bla: Record<string, any>) {
  return bla.children.reduce((accum: number, child: Record<string, any>) => {
    if (isSelected(child)) {
      return accum + 1;
    }
    return accum;
  }, 0);
}

const isIndeterminate = computed<boolean>(() => {
  if (selected.value) return false;
  if (childIsIndeterminate.value) return true;
  if (!option.value.children) return false;

  return (
    numberOfSelectedChildren.value > 0 &&
    numberOfSelectedChildren.value <= option.value.children.length
  );
});

watch(isIndeterminate, signalParentOurIndeterminateStatus);

watch(filter, (newFilter: string) => {
  open.value = !!newFilter;
});

function selectOption(checked: boolean, option: Record<string, any>) {
  filtersStore.updateOntologyFilter(facetIdentifier.value, option, checked);
}

function handleChildIndeterminateUpdate(newStatus: boolean) {
  childIsIndeterminate.value = newStatus;
}

function signalParentOurIndeterminateStatus() {
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
