<template>
  <div class="d-flex">
    <div class="ml-auto d-flex">
      <label :for="idOption1">
        <input
          type="radio"
          :id="idOption1"
          :name="selector"
          v-model="matchType"
          :value="option1"
        />
        {{ uiText["match_option1"] }}
      </label>
      <label :for="idOption2">
        <input
          type="radio"
          :id="idOption2"
          :name="selector"
          v-model="matchType"
          :value="option2"
        />
        {{ uiText["match_option2"] }}
      </label>
    </div>
  </div>
</template>

<script>
import { useFiltersStore } from "../../../stores/filtersStore";
import { useSettingsStore } from "../../../stores/settingsStore";
export default {
  setup() {
    const settingsStore = useSettingsStore();
    const filtersStore = useFiltersStore();
    return { settingsStore, filtersStore };
  },
  props: {
    matchTypeForFilter: {
      type: String,
      required: true,
    },
    option1: {
      type: String,
      default: () => "all",
    },
    option2: {
      type: String,
      default: () => "any",
    },
  },
  data: function () {
    return {
      /** need to generate random ids so that every filter has its own radiobutton group */
      selector: new Date().getMilliseconds() + Math.random(),
      idOption1: new Date().getMilliseconds() + Math.random(),
      idOption2: new Date().getMilliseconds() + Math.random(),
    };
  },
  computed: {
    uiText() {
      return this.settingsStore.uiText;
    },
    matchType: {
      get() {
        return (
          this.filtersStore.getFilterType(this.matchTypeForFilter) || "any"
        );
      },
      set(value) {
        this.filtersStore.updateFilterType(this.matchTypeForFilter, value);
      },
    },
  },
};
</script>

<style scoped>
div label:first-child {
  margin-right: 1rem;
}
input {
  position: relative;
  bottom: 1px;
}
</style>
