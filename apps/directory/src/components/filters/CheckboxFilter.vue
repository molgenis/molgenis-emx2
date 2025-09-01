<template>
  <div>
    <MatchTypeRadiobutton
      v-if="showMatchTypeSelector"
      class="p-2 pb-0"
      :matchTypeForFilter="facetIdentifier"
    />

    <div class="d-flex flex-column scrollable-content pt-2">
      <CheckboxComponent
        v-for="(option, index) of checkboxOptions"
        :key="index"
        v-model="filterSelection"
        :option="option"
      />
    </div>
    <div>
      <button
        v-if="selectAll"
        type="button"
        class="btn btn-link p-2"
        @click.prevent="toggleSelect"
      >
        {{ selectAllText }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { IFilterOption } from "../../interfaces/interfaces";
import { useFiltersStore } from "../../stores/filtersStore";
import { useSettingsStore } from "../../stores/settingsStore";
import CheckboxComponent from "./base/CheckboxComponent.vue";
import MatchTypeRadiobutton from "./base/MatchTypeRadiobutton.vue";

const settingsStore = useSettingsStore();
const filtersStore = useFiltersStore();

const props = defineProps<{
  facetTitle: string;
  facetIdentifier: string;
  options: () => Promise<Array<{ text: string; value: string }>>;
  currentlyActive?: boolean;
  optionsFilter?: Array<string>;
  selectAll?: boolean;
  showMatchTypeSelector?: boolean;
}>();

const resolvedOptions = ref<Array<{ text: string; value: string }>>([]);
const reducedOptions = ref<Array<string>>(props.optionsFilter || []);

props.options().then((response) => {
  resolvedOptions.value = response;
});

watch(
  () => props.optionsFilter,
  (newValue) => {
    if (
      !props.currentlyActive ||
      (filtersStore.getFilterValue(props.facetIdentifier) || []).length === 0
    ) {
      reducedOptions.value = newValue || [];
    }
  },
  { immediate: true }
);

const selectAllText = computed(() => {
  if (filterSelection.value && filterSelection.value.length > 0) {
    return settingsStore.uiText.value["deselect_all"];
  } else {
    return settingsStore.uiText.value["select_all"];
  }
});

const checkboxOptions = computed(() => {
  if (reducedOptions.value.length) {
    const selectedValues = filterSelection.value.map(
      (selection: IFilterOption) => selection.value
    );

    return resolvedOptions.value.filter(
      (option) =>
        reducedOptions.value.includes(option.value) ||
        selectedValues.includes(option.value)
    );
  } else {
    return resolvedOptions.value;
  }
});

const filterSelection = computed({
  get() {
    return filtersStore.getFilterValue(props.facetIdentifier) || [];
  },
  set(value) {
    filtersStore.updateFilter(props.facetIdentifier, value);
  },
});

function toggleSelect() {
  if (filterSelection.value?.length) {
    filterSelection.value = [];
  } else {
    filterSelection.value = checkboxOptions.value;
  }
}
</script>

<style scoped>
.btn-link:focus {
  box-shadow: none;
}
</style>
