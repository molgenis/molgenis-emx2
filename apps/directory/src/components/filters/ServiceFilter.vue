<template>
  <div>
    <MatchTypeRadiobutton
      class="p-2 pb-0"
      :matchTypeForFilter="facetIdentifier"
    />

    <div class="d-flex flex-column scrollable-content pt-2">
      <div v-for="groupName of Object.keys(groupedOptions).sort()">
        <div>
          <b>{{ groupLabels[groupName] }}</b>
        </div>
        <div v-for="option of groupedOptions[groupName]">
          <CheckboxComponent v-model="filterSelection" :option="option" />
        </div>
      </div>
    </div>
    <div>
      <button
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
import { computed, onMounted, ref } from "vue";
import { IFilterOption } from "../../interfaces/interfaces";
import { useFiltersStore } from "../../stores/filtersStore";
import { useSettingsStore } from "../../stores/settingsStore";
import CheckboxComponent from "./base/CheckboxComponent.vue";
import MatchTypeRadiobutton from "./base/MatchTypeRadiobutton.vue";

const filtersStore = useFiltersStore();
const settingsStore = useSettingsStore();

const { facetIdentifier, options } = defineProps<{
  facetIdentifier: string;
  options: Function;
  currentlyActive?: boolean;
  optionsFilter?: IOption[];
}>();

const resolvedOptions = ref<IOption[]>([]);
const groupedOptions = ref<Record<string, IOption[]>>({});
const groupLabels = ref<Record<string, string>>({});

onMounted(() => {
  options().then((response: IOption[]) => {
    resolvedOptions.value = response;
    groupedOptions.value = groupOptions(response);
  });
});

function groupOptions(options: IOption[]) {
  const groupedOptions = options.reduce(
    (accum: Record<string, IOption[]>, option) => {
      const name = option.extraAttributes["serviceCategory.name"];
      const label = option.extraAttributes["serviceCategory.label"];
      if (!accum[name]) {
        accum[name] = [];
        groupLabels.value[name] = label;
      }
      accum[name].push(option);
      return accum;
    },
    {}
  );

  Object.keys(groupedOptions).forEach((key) => {
    groupedOptions[key] = groupedOptions[key].sort();
  });

  return groupedOptions;
}

const filterSelection = computed({
  get() {
    return filtersStore.getFilterValue(facetIdentifier) || [];
  },
  set(filters) {
    const filterOptions: IFilterOption[] = filters.map((filter: IOption) => {
      return { text: filter.label, value: filter.value };
    });
    filtersStore.updateFilter(facetIdentifier, filterOptions);
  },
});

const selectAllText = computed(() => {
  if (filterSelection.value?.length) {
    return settingsStore.uiText["deselect_all"];
  } else {
    return settingsStore.uiText["select_all"];
  }
});

function toggleSelect() {
  if (filterSelection.value?.length) {
    filterSelection.value = [];
  } else {
    filterSelection.value = resolvedOptions.value;
  }
}

interface IOption {
  label: string;
  value: string;
  extraAttributes: {
    "serviceCategory.name": string;
    "serviceCategory.label": string;
  };
}
</script>

<style scoped>
.btn-link:focus {
  box-shadow: none;
}
</style>
