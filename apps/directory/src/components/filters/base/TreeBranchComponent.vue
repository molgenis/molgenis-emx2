<template>
  <ul>
    <li @click="open = !open" :class="option.children ? 'clickable' : ''">
      <!-- because Vue3 does not allow me, for some odd reason, to toggle a class or spans with font awesome icons, we have to do it like  -->
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
        :indeterminate.prop="indeterminateState"
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
        @indeterminate-update="signalParentOurIndeterminateStatus"
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

const { option, filter, parentSelected, facetIdentifier } = toRefs(props);

const open = ref<boolean>(!!filter.value);
const childIsIndeterminate = ref<boolean>(false);

const currentFilterSelection = computed<any[]>(() => {
  return filtersStore.getFilterValue(facetIdentifier.value) || [];
});

const selected = computed(() => {
  if (parentSelected.value) {
    return true;
  } else if (!currentFilterSelection.value?.length) {
    return false;
  } else {
    return currentFilterSelection.value.some(
      (selectedValue: Record<string, any>) =>
        selectedValue.name === option.value.name
    );
  }
});

const numberOfChildrenInSelection = computed(() => {
  if (!option.value.children) {
    return 0;
  }

  return selectedChildren.value.length;
});

const selectedChildren = computed(() => {
  const childNames = option.value.children.map(
    (childOption: Record<string, any>) => childOption.name
  );
  return currentFilterSelection.value.filter(
    (selectedOption: Record<string, any>) =>
      childNames.includes(selectedOption.name)
  );
});

const indeterminateState = computed(() => {
  if (parentSelected.value) return false;
  if (childIsIndeterminate.value) return true;
  if (!option.value.children) return false;

  return (
    numberOfChildrenInSelection.value !== option.value.children.length &&
    numberOfChildrenInSelection.value > 0
  );
});

watch(indeterminateState, (status: boolean) => {
  signalParentOurIndeterminateStatus(status);
});

watch(filter, (newFilter: string) => {
  open.value = !!newFilter;
});

function selectOption(checked: boolean, option: Record<string, any>) {
  /** if it is checked we add */
  filtersStore.updateOntologyFilter(facetIdentifier.value, option, checked);
  signalParentOurIndeterminateStatus(checked);
}

function signalParentOurIndeterminateStatus(status: boolean) {
  childIsIndeterminate.value = status;
  emit("indeterminate-update", status);
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
