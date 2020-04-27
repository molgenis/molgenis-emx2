<template>
  <div class="card">
    <div ref="header" class="card-header text-center">
      <h6>{{ title }}</h6>
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

<style scoped>
.filter-collapse-icon {
  float: right;
  position: absolute;
  top: 0px;
  right: 0px;
}

.card-header h6 {
  margin: 0px;
}
</style>

<script>
import IconAction from './IconAction'

export default {
  components: {
    IconAction
  },
  props: {
    title: String,
    collapsed: Boolean
  }
}
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
