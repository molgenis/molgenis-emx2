<template>
  <ul>
    <li @click="open = !open">
      <input @click.stop type="checkbox" :ref="`${option.name}-checkbox`" class="mr-1" />
      <label>
        {{ option.label }}
      </label>
      <!-- because Vue3 does not allow me, for some odd reason, to toggle a class or spans with font awesome icons, we have to do it like this. -->
      <span class="toggle-icon">
        {{ option.children ? open ? "&#9660;" : "&#9650;" : '' }}
      </span>
    </li>
    <li
      v-if="option.children"
      class="border border-right-0 border-bottom-0 border-top-0 indent"
    >
      <tree-component v-if="open" :options="option.children" />
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
  props: {
    option: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      open: false,
    };
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
  margin-left: 0.5rem;
}

.indent {
  margin-left: 0.35rem;
  border-width: 2px !important;
}
</style>