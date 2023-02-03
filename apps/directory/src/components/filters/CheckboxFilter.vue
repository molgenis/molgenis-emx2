<template>
  <div>
    <MatchTypeRadiobutton
      v-if="showMatchTypeSelector"
      class="p-2"
      :matchTypeForFilter="facetTitle"
    />

    <div class="d-flex flex-column scrollable-content">
      <CheckboxComponent
        v-for="(option, index) of checkboxOptions"
        :key="index"
        v-model="selection"
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

<script>
import MatchTypeRadiobutton from "./base/MatchTypeRadiobutton.vue";
import CheckboxComponent from "./base/CheckboxComponent.vue";
import { useSettingsStore } from "../../stores/settingsStore";

export default {
  setup() {
    const settingsStore = useSettingsStore();
    return { settingsStore };
  },
  name: "CheckboxFilter",
  components: {
    MatchTypeRadiobutton,
    CheckboxComponent,
  },
  props: {
    facetTitle: {
      type: String,
      required: true,
    },
    /**
     * A Promise-function that resolves with an array of options.
     * { text: 'foo', value: 'bar' }
     */
    options: {
      type: [Function],
      required: true,
    },
    /**
     * An array that contains values of options
     * which is used to only show the checkboxes that match
     * these values
     */
    optionsFilter: {
      type: Array,
      required: false,
    },
    /**
     * This is the v-model value; an array of selected options.
     * Can also be a { text, value } object array
     */
    modelValue: {
      type: Array,
      default: () => [],
    },
    /**
     * Whether to use (De)Select All or not.
     */
    selectAll: {
      type: Boolean,
      required: false,
      default: () => true,
    },
    showMatchTypeSelector: {
      type: Boolean,
      default: () => false,
    },
  },
  data() {
    return {
      selection: [],
      resolvedOptions: [],
    };
  },
  computed: {
    uiText() {
      return this.settingsStore.uiText;
    },
    selectAllText() {
      if (this.selection && this.selection.length > 0) {
        return this.uiText["deselect_all"];
      } else {
        return this.uiText["select_all"];
      }
    },
    checkboxOptions() {
      if (this.optionsFilter && this.optionsFilter.length) {
        return this.resolvedOptions.filter((option) =>
          this.optionsFilter.includes(option.value)
        );
      } else {
        return this.resolvedOptions;
      }
    },
  },
  methods: {
    toggleSelect() {
      if (this.selection && this.selection.length > 0) {
        this.selection = [];
      } else {
        this.selection = this.checkboxOptions;
      }
    },
  },
  created() {
    this.options().then((response) => {
      this.resolvedOptions = response;
    });
  },
};
</script>

<style scoped>
.btn-link:focus {
  box-shadow: none;
}
</style>