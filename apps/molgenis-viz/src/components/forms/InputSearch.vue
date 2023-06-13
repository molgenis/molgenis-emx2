<template>
  <div>
    <InputLabel :id="id" :label="label" :description="description" />
    <input
      :id="id"
      name="query"
      type="search"
      class="input__search"
      v-model="value"
      @submit.prevent
      @input="$emit('search', value)"
    />
  </div>
</template>

<script>
import InputLabel from '@/components/forms/InputLabel.vue'

// @displayName InputSearch
// Create search field for forms. User input is emitted and accessible using a search event: `<InputSearch @search="(value) => someFunction(value)" />`.
export default {
  components: {
    InputLabel
  },
  props: {
    // a unique identifier for the search field
    id: {
      type: String,
      required: true
    },
    // a label, or title, that describes the search field
    label: {
      type: String,
      required: true
    },
    // an optional description that provides context for the search field
    description: {
      type: String,
      required: false
    }
  },
  data () {
    return {
      value: null
    }
  }
}
</script>

<style lang="scss" scoped>
.input__search {
  display: block;
  width: 100%;
  background: none;
  border: none;
  border: 1px solid $gray-200;
  padding: 6px;
  padding-left: 12px;
  border-radius: 4px;
  box-shadow: $box-shadow-inset;
  margin: 12px 0;
}
</style>
