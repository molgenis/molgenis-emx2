<template>
  <div class="mb-2 pt-2 border-top bg-white">
    <div
      class="filter-header d-flex flex-row justify-content-between"
      @click="expandedState = !expandedState"
    >
      <h6 class="mb-0 font-weight-bold p-2">
        {{ title }}
        <span v-if="count > 0" class="badge badge-secondary">{{ count }}</span>
      </h6>
      <IconAction
        class="ml-auto"
        :icon="expandedState ? 'angle-up' : 'angle-down'"
      />
    </div>
    <slot name="header" />
    <div v-if="expandedState" class="p-2">
      <!-- @slot Use this slot to place the filter box content -->
      <slot />
    </div>
  </div>
</template>

<style scoped>
.filter-header h6 {
  vertical-align: top;
}
</style>

<script>
import IconAction from "../forms/IconAction.vue";

export default {
  components: {
    IconAction,
  },
  data() {
    return {
      expandedState: this.expanded,
    };
  },
  props: {
    title: String,
    conditions: Array,
    expanded: {
      type: Boolean,
      default: true,
    },
  },
  computed: {
    count() {
      return Array.isArray(this.conditions) ? this.conditions.length : null;
    },
  },
};
</script>

<docs>
<template>
  <div>
    <label>Example expanded</label>
    <demo-item>
      <FilterContainer title="My filter" :visible="true" :expanded="true">
        some contents of the filter box
      </FilterContainer>
    </demo-item>
    <label>Example collapsed</label>
    <demo-item>
      <FilterContainer title="My filter" :visible="true" :expanded="false">
        some contents of the filter box
      </FilterContainer>
    </demo-item>
  </div>
</template>
</docs>
