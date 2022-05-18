<template>
  <InputRef
    :id="id"
    :value="condition"
    @input="onUpdateCondition"
    :tableName="tableName"
    :graphqlURL="graphqlURL"
  />
</template>

<script>
import InputRef from "../forms/InputRef.vue";

export default {
  name: "RefFilter",
  components: { InputRef },
  props: {
    id: {
      type: String,
      required: true,
    },
    name: {
      type: String,
      required: false,
      default: function () {
        return this.id;
      },
    },
    condition: {
      type: Object,
      required: false,
    },
    graphqlURL: {
      required: false,
      type: String,
    },
    tableName: {
      type: String,
      required: true,
    },
  },
  methods: {
    onUpdateCondition($event) {
      this.$emit("updateCondition", $event);
    },
  },
};
</script>

<style>
</style>