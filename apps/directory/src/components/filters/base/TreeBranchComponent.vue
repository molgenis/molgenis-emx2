<template>
  <ul>
    <li @click="open = !open" :class="option.children ? 'clickable' : ''">
      <!-- because Vue3 does not allow me, for some odd reason, to toggle a class or spans with font awesome icons, we have to do it like this. -->
      <span class="toggle-icon">
        {{ option.children ? (open ? "&#9660;" : "&#9650;") : "" }}
      </span>
      <input
        @click.stop
        @change="(e:Event) => selectOption(e.target?.checked, option)"
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
import { computed, defineAsyncComponent, ref } from "vue";
import { IOntologyItem } from "../../../filter-config/filterOptions";
import { useFiltersStore } from "../../../stores/filtersStore";

/** need to lazy load because it gets toooo large quickly. Over 9000! */
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
  { parentSelected: false, filter: "" }
);
const { facetIdentifier, option, filter } = props;

let open = ref(false);
let childIsIndeterminate = ref(false);

let currentFilterSelection = computed<string[]>(() => {
  return filtersStore.getFilterValue(facetIdentifier) || [];
});

let selected = computed<boolean>(() => {
  if (props.parentSelected) {
    console.log(props.parentSelected);
    return true;
  } else if (
    !currentFilterSelection.value ||
    !currentFilterSelection.value.length
  ) {
    return false;
  } else {
    return currentFilterSelection.value.some(
      (selectedValue: string) => selectedValue === option.name
    );
  }
});

let numberOfChildrenInSelection = computed<number>(() => {
  if (!option.children) return 0;
  const childNames = option.children.map(
    (childOption: IOntologyItem) => childOption.name
  );
  const selectedChildren = currentFilterSelection.value.filter(
    (selectedOption: string) => childNames.includes(selectedOption)
  );
  return selectedChildren.length;
});

let indeterminateState = computed<boolean>(() => {
  const state = getIndeterminateState(
    props.parentSelected,
    childIsIndeterminate.value,
    option,
    numberOfChildrenInSelection.value
  );
  emit("indeterminate-update", state);
  childIsIndeterminate.value = state;
  return state;
});

function selectOption(checked: boolean, option: IOntologyItem) {
  filtersStore.updateOntologyFilter(facetIdentifier, option.name, checked);
}

function signalParentOurIndeterminateStatus(status: boolean) {
  childIsIndeterminate.value = status;
}

function getIndeterminateState(
  parentSelected: boolean,
  childIsIndeterminate: boolean,
  option: IOntologyItem,
  numberOfChildrenInSelection: number
) {
  if (parentSelected) return false;
  if (childIsIndeterminate) return true;
  if (!option.children) return false;

  return (
    numberOfChildrenInSelection !== option.children.length &&
    numberOfChildrenInSelection > 0
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
