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
        :checked="isSelected"
        :indeterminate.prop="isIndeterminate"
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

const indeterminateDiseases = computed(
  () => filtersStore.indeterminateDiseases
);
const selectedDiseases = computed(() => filtersStore.selectedDiseases);
const numberOfSelectedChildren = computed(getNumberOfSelectedChildren);

const isSelected = computed<boolean>(
  () => selectedDiseases.value[option.value.name]
);

const isIndeterminate = computed<boolean>(() => {
  if (lessThenAllChildrenSelected() || hasIndeterminateChild()) {
    return true;
  } else {
    return false;
  }
});

watch(filter, (newFilter: string) => (open.value = !!newFilter));
watch(
  isIndeterminate,
  (newValue: boolean) => {
    filtersStore.setDiseaseIndeterminate(option.value.name, newValue);
  },
  { immediate: true }
);
watch(numberOfSelectedChildren, (newValue) => {
  if (!isSelected.value && newValue === option.value.children?.length) {
    selectOption(true, option.value);
  }
});

function hasIndeterminateChild(): boolean {
  return !!option.value.children?.some((child) => {
    return indeterminateDiseases.value[child.name];
  });
}

function lessThenAllChildrenSelected(): boolean {
  return (
    numberOfSelectedChildren.value > 0 &&
    numberOfSelectedChildren.value < (option.value.children?.length ?? Infinity)
  );
}

function selectOption(checked: boolean, option: IOntologyItem) {
  filtersStore.updateOntologyFilter(facetIdentifier.value, option, checked);
}

function getNumberOfSelectedChildren() {
  return (
    option.value.children?.filter(
      (child: IOntologyItem) => selectedDiseases.value[child.name]
    ).length || 0
  );
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
