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

const selectedFilters = computed<any[]>(() => {
  return filtersStore.getFilterValue(facetIdentifier.value) || [];
});

const selected = computed(() => {
  if (parentSelected.value) {
    return true;
  } else if (selectedChildren.value.length === option.value.children?.length) {
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

const selectedChildren = computed(() => {
  const childNames =
    option.value.children?.map(
      (childOption: Record<string, any>) => childOption.name
    ) || [];
  return selectedFilters.value.filter((selectedFilter: Record<string, any>) =>
    childNames.includes(selectedFilter.name)
  );
});

const isIndeterminate = computed<boolean>(() => {
  if (parentSelected.value) return false;
  if (childIsIndeterminate.value) return true;
  if (!option.value.children) return false;

  return (
    selectedChildren.value.length > 0 &&
    selectedChildren.value.length < option.value.children.length
  );
});

watch(isIndeterminate, signalParentOurIndeterminateStatus);

watch(filter, (newFilter: string) => {
  open.value = !!newFilter;
});

function selectOption(checked: boolean, option: Record<string, any>) {
  filtersStore.updateOntologyFilter(facetIdentifier.value, option, checked);
  // emit("indeterminate-update", checked || isIndeterminate.value);
}

function handleChildIndeterminateUpdate(newStatus: boolean) {
  childIsIndeterminate.value = newStatus;
}

function signalParentOurIndeterminateStatus() {
  emit("indeterminate-update", isIndeterminate);
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
