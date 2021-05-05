<template>
  <b-input-group>
    <b-input-group-prepend>
      <b-input-group-text>
        <font-awesome-icon icon="search" />
      </b-input-group-text>
    </b-input-group-prepend>
    <b-form-input
        :value="search"
        :placeholder="placeholder"
        @input="input"
        @change="change"
    />
    <b-input-group-append>
      <b-button
          @click="clear"
      >
        <font-awesome-icon icon="times" />
      </b-button>
    </b-input-group-append>
  </b-input-group>
</template>

<script>
export default {
  props: {
    search: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: 'Search...'
    }
  },
  methods: {
    input (arg) {
      this.$emit('input', arg)
    },
    change (arg) {
      this.$emit('change', arg)
    },
    clear (arg) {
      this.$emit('input', '')
      this.$emit('change', '')
    }
  }
}
</script>

<style>
</style>
