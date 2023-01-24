<template>
  <div>
    <MatchTypeRadiobutton v-if="showSatisfyAllSelector" :matchTypeForFilter="filterName"/>
    <b-form-checkbox-group
      class="checkbox-group"
      v-model="selection"
      stacked
      :options="visibleOptions"
    />
    <span v-if="bulkOperation">
      <b-link
        v-if="showToggleSlice"
        class="toggle-slice card-link"
        @click.prevent="toggleSlice"
      >
        {{ toggleSliceText }}
      </b-link>
      <b-link class="toggle-select card-link" @click.prevent="toggleSelect">
        {{ toggleSelectText }}
      </b-link>
    </span>
  </div>
</template>

<script>
import MatchTypeRadiobutton from "./micro-components/MatchTypeRadiobutton.vue";

export default {
  name: "CheckboxFilter",
  components: {
    MatchTypeRadiobutton,
  },
  props: {
    filterName: {
      type: String,
      required: true
    },
    /**
     * Toggle to switch between returning an array with values or an array with the full option
     */
    returnTypeAsObject: {
      type: Boolean,
      required: false,
      default: () => false,
    },
    /**
     * A Promise-function that resolves with an array of options.
     * {text: 'foo', value: 'bar'}
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
    value: {
      type: Array,
      default: () => [],
    },
    /**
     * Whether to use (De)Select All or not.
     */
    bulkOperation: {
      type: Boolean,
      required: false,
      default: () => true,
    },
    /**
     * Limit the maximum number of visible items.
     */
    maxVisibleOptions: {
      type: Number,
      default: () => undefined,
    },
  },
  data() {
    return {
      externalUpdate: false,
      selection: [],
      resolvedOptions: [],
      sliceOptions:
        this.maxVisibleOptions &&
        this.optionsToRender &&
        this.maxVisibleOptions < this.optionsToRender.length,
    };
  },
  computed: {
    visibleOptions() {
      return this.sliceOptions
        ? this.optionsToRender.slice(0, this.maxVisibleOptions)
        : typeof this.optionsToRender === "function"
        ? []
        : this.optionsToRender;
    },
    showToggleSlice() {
      return (
        this.maxVisibleOptions &&
        this.maxVisibleOptions < this.optionsToRender.length
      );
    },
    toggleSelectText() {
      return this.value.length ? "Deselect all" : "Select all";
    },
    toggleSliceText() {
      return this.sliceOptions
        ? `Show ${this.optionsToRender.length - this.maxVisibleOptions} more`
        : "Show less";
    },
    optionsToRender() {
      if (this.optionsFilter && this.optionsFilter.length) {
        return this.resolvedOptions.filter((option) =>
          this.optionsFilter.includes(option.value)
        );
      } else {
        return this.resolvedOptions;
      }
    },
  },
  watch: {
    value() {
      this.setValue();
    },
    resolvedOptions() {
      this.sliceOptions = this.showToggleSlice;
    },
    selection(newValue) {
      if (!this.externalUpdate) {
        let newSelection = [];

        if (this.returnTypeAsObject) {
          newSelection = Object.assign(
            newSelection,
            this.optionsToRender.filter((of) => newValue.includes(of.value))
          );
        } else {
          newSelection = [...newValue];
        }
        this.$emit("input", newSelection);
      }
      this.externalUpdate = false;
    },
  },
  created() {
    this.options().then((response) => {
      this.resolvedOptions = response;
    });
    this.setValue();
  },
  methods: {
    toggleSelect() {
      if (this.selection && this.selection.length > 0) {
        this.selection = [];
      } else {
        this.selection = this.optionsToRender.map((option) => option.value);
      }
    },
    toggleSlice() {
      this.sliceOptions = !this.sliceOptions;
    },
    setValue() {
      this.externalUpdate = true;
      if (
        this.value &&
        this.value.length > 0 &&
        typeof this.value[0] === "object"
      ) {
        this.selection = this.value.map((vo) => vo.value);
      } else {
        this.selection = this.value;
      }
    },
  },
};
</script>

<style>
.checkbox-group {
  max-height: 15rem;
  overflow-y: auto;
  padding-left: 0.25rem;
}

.card-link {
  font-size: small;
  font-style: italic;
}
</style>
