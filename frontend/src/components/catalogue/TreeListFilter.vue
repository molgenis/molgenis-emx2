<template>
  <div>
    <IconAction :icon="expand ? 'caret-up' : 'caret-down'" @click="toggle" />
    <strong class="font-weight-bold">{{ value.name }}</strong>
    <div v-if="expand">
      <Spinner v-if="loading" />
      <div v-for="option in value.options" v-else class="ml-4">
        <input v-model="option.checked" type="checkbox">
        {{ option[value.refColumn] }}
      </div>
    </div>
  </div>
</template>

<script>
import {request} from 'graphql-request'
import {IconAction, Spinner} from '@mswertz/emx2-styleguide'

export default {
  components: {IconAction, Spinner},
  props: {
    value: Object,
  },
  data() {
    return {
      expand: false,
      loading: false,
    }
  },
  methods: {
    toggle() {
      if (this.value.options == undefined) {
        this.loading = true
        request(
          'graphql',
          '{' + this.value.refTable + '{' + this.value.refColumn + '}}',
        )
          .then((data) => {
            this.value.options = data[this.value.refTable]
          })
          .catch((error) => {
            this.graphqlError = error.response.errors[0].message
          })
          .finally(() => {
            this.loading = false
          })
      }
      this.expand = !this.expand
    },
  },
}
</script>
