<template>
  <ul>
    <li @click="open = !open" :class="option.children ? 'clickable' : ''">
      <!-- because Vue3 does not allow me, for some odd reason, to toggle a class or spans with font awesome icons, we have to do it like this. -->
      <span class="toggle-icon">
        {{ option.children ? (open ? "&#9660;" : "&#9650;") : "" }}
      </span>
      <input
        @click.stop
        @change="(e) => selectOption(e.target.checked, option)"
        type="checkbox"
        :ref="`${option.name}-checkbox`"
        class="mr-1"
        :checked="selected"
        :indeterminate.prop="indeterminateState"
      />
      <label> {{ option.code }} {{ option.label }} </label>
    </li>
    <li
      v-if="option.children"
      class="border border-right-0 border-bottom-0 border-top-0 indent"
    >
      <tree-component
        v-if="open"
        :options="option.children"
        :facetIdentifier="facetIdentifier"
        @indeterminate-update="signalParentOurIndeterminateStatus"
      />
    </li>
  </ul>
</template>

<script>
import { defineAsyncComponent } from "vue";
import { useSettingsStore } from "../../../stores/settingsStore";
import { useFiltersStore } from "../../../stores/filtersStore";

/** need to lazy load because it gets toooo large quickly. Over 9000! */
const TreeComponent = defineAsyncComponent(() => import("./TreeComponent.vue"));
export default {
  setup() {
    const settingsStore = useSettingsStore();
    const filtersStore = useFiltersStore();
    return { settingsStore, filtersStore };
  },
  emits: ["indeterminate-update"],
  props: {
    facetIdentifier: {
      type: String,
      required: true,
    },
    option: {
      type: Object,
      required: true,
    },
  },
  name: "TreeBranchComponent",
  components: {
    TreeComponent,
  },
  data() {
    return {
      open: false,
      childIsIndeterminate: false,
      selectedChildren: [],
    };
  },
  watch: {
    indeterminateState(status) {
      this.signalParentOurIndeterminateStatus(status);
    },
  },
  computed: {
    currentFilterSelection() {
      if (!this.filtersStore) return [];
      return this.filtersStore.getFilterValue(this.facetIdentifier) || [];
    },
    selected() {
      if (!this.currentFilterSelection || !this.currentFilterSelection.length)
        return false;
      else
        return this.currentFilterSelection.some(
          (selectedValue) => selectedValue.name === this.option.name
        );
    },
    numberOfChildrenInSelection() {
      if (!this.option.children) return 0;
      const childNames = this.option.children.map(
        (childOption) => childOption.name
      );
      const selectedChildren = this.currentFilterSelection.filter(
        (selectedOption) => childNames.includes(selectedOption.name)
      );
      return selectedChildren.length;
    },
    indeterminateState() {
      if (this.childIsIndeterminate) return true;
      if (!this.option.children) return false;

      return (
        this.numberOfChildrenInSelection !== this.option.children.length &&
        this.numberOfChildrenInSelection > 0
      );
    },
  },

  methods: {
    selectOption(checked, option) {
      /** if it is checked we add */
      this.filtersStore.updateOntologyFilter(
        this.facetIdentifier,
        option,
        checked
      );
    },
    signalParentOurIndeterminateStatus(status) {
      this.childIsIndeterminate = status;
      this.$emit("indeterminate-update", status);
    },
  },
};
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

.indent {
  margin-left: 0.25rem;
  border-width: 2px !important;
}

.clickable:hover > label {
  cursor: pointer;
  background-color: var(--gray-light);
}
</style>