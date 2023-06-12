<script setup>
defineEmits(["checkbox-update"]);
defineProps(["parentSelected", "option"])
</script>

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
        :indeterminate.prop="indeterminateState"
      />
      <label> {{ option.code }} {{ option.label }} </label>
    </li>
    <li
      v-if="option.children"
      class="border border-right-0 border-bottom-0 border-top-0 indent"
    >
      <tree-component
        @checkbox-update="handleChildSelection"
        v-if="open"
        :options="option.children"
        :parentSelected="parentState"
      />
    </li>
  </ul>
</template>

<script>
import { defineAsyncComponent } from "vue";
/** need to lazy load because of recursion */
const TreeComponent = defineAsyncComponent(() => import("./TreeComponent.vue"));
export default {
  name: "TreeBranchComponent",
  components: {
    TreeComponent,
  },
  data() {
    return {
      open: false,
      isChecked: false,
      indeterminateState: false,
      selectedChildren: [],
    };
  },
  watch: {
    parentSelected(value) {
      console.log(value)
      this.isChecked = value;
      this.$refs[`${this.option.name}-checkbox`].checked = value;
    },
  },
  computed: {
    parentState() {
      return this.isChecked
    },
    allChildrenSelected() {
      if (!this.option.children) return true;
      else return this.option.children.length === this.selectedChildren.length;
    },
  },

  methods: {
    selectOption(checked, option) {
      console.log(checked, 'hello')
      this.isChecked = checked;
      this.$emit("checkbox-update", { checked, option });
    },
    handleChildSelection(update) {
      if (update.checked) {
        this.selectedChildren.push(update.option);
        this.indeterminateState = this.allChildrenSelected ? false : true;
      } else {
        this.selectedChildren = this.selectedChildren.filter(
          (selected) => selected.label !== update.option.label
        );
      }

      if (this.allChildrenSelected) {
        this.$refs[`${this.option.name}-checkbox`].checked = true;
        this.isChecked = true;
        this.$emit("checkbox-update", { checked: true, option: this.option });
      } else if (this.selectedChildren.length > 0) {
        this.indeterminateState = true;
      } else {
        this.indeterminateState = false;
        this.$refs[`${this.option.name}-checkbox`].checked = false;
        this.isChecked = false;
        this.$emit("checkbox-update", { checked: false, option: this.option });
      }
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