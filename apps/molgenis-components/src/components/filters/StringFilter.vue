<template>
  <InputString
    :id="id"
    :value="condition"
    @input="onUpdateCondition"
  />
</template>

<script>
import InputString from "../forms/InputString.vue";

export default {
  name: 'StringFilter',
  components: { InputString },
  props: {
    id: {
      type: String,
      required: true
    },
    name: {
      type: String,
      required: false,
      default: function () {
        return this.id;
      }
    },
    condition: {
      type: String,
      required: false
    }
  },
  methods: {
    onUpdateCondition ($event) {
      this.$emit('updateCondition', $event)
    }
  }
};
</script>

<style>
</style>