<template>
  <div>
    <MatchTypeRadiobutton
      v-if="showMatchTypeSelector"
      :matchTypeForFilter="filterName"
    />

    <div>
      <CheckboxComponent
        v-for="(option, index) of resolvedOptions"
        :key="index"
        v-model="selection"
        :option="option"
      />
    </div>

    <!-- <span v-if="bulkOperation">
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
    </span> -->
  </div>
</template>

<script>
import MatchTypeRadiobutton from "./micro-components/MatchTypeRadiobutton.vue";
import CheckboxComponent from "./micro-components/CheckboxComponent.vue";

export default {
  name: "CheckboxFilter",
  components: {
    MatchTypeRadiobutton,
    CheckboxComponent,
  },
  props: {
    filterName: {
      type: String,
      required: true,
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
    modelValue: {
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
      default: () => 20,
    },
    showMatchTypeSelector: {
      type: Boolean,
      default: () => false,
    },
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
    resolvedOptions() {
      this.sliceOptions = this.showToggleSlice;
    },
  },
  data() {
    return {
      externalUpdate: false,
      selection: [],
      resolvedOptions: [],
      sliceOptions: false,
    };
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
      this.selection = Object.assign([], this.modelValue);
    },
  },
  created() {
    this.options().then((response) => {
      this.resolvedOptions = response;
    });

    this.sliceOptions =
      this.maxVisibleOptions &&
      this.optionsToRender &&
      this.maxVisibleOptions < this.optionsToRender.length;
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
