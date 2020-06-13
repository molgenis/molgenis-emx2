<template>
  <div class="card mb-3">
    <div ref="header" class="card-header text-center" @click="$emit('click')">
      <IconAction class="filter-drag-icon" icon="ellipsis-v" />
      <h6 @click="$emit('click')">{{ title }}</h6>
      <slot name="header" />
      <IconAction
        class="filter-collapse-icon"
        :icon="collapsed ? 'caret-down' : 'caret-up'"
        @click="collapsed ? $emit('uncollapse') : $emit('collapse')"
      />
    </div>
    <div v-if="!collapsed" class="card-body">
      <!-- @slot Use this slot to place the filter box content -->
      <slot />
    </div>
  </div>
</template>

<style>
.filter-drag-icon {
  float: left;
  position: absolute;
  top: 0px;
  left: 0px;
  visibility: hidden;
}

.card-header:hover .filter-drag-icon:hover {
  cursor: move;
}

.card-header:hover .filter-drag-icon,
.card-header:hover .filter-collapse-icon {
  visibility: visible;
}

.filter-collapse-icon {
  float: right;
  position: absolute;
  top: 0px;
  right: 0px;
  visibility: hidden;
}

.card-header h6 {
  margin: 0px;
}
</style>

<script>
    import IconAction from "./IconAction";

    export default {
  components: {
    IconAction
  },
  props: {
    title: String,
    collapsed: Boolean
  }
};
</script>

<docs>

    Example

    ```jsx
    <template>
        <FilterContainer title="My filter" :collapsed="collapsed" @collapse="collapsed=true"
                         @uncollapse="collapsed=false">
            some contents
        </FilterContainer>
    </template>
    <script>
        export default {
            data() {
                return {
                    collapsed: false
                }
            }
        }
    </script>
    ```

</docs>
