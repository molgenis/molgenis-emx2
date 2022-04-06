<template>
  <InputString :id="id" :value="condition" @input="onUpdateCondition">
    <template v-slot:append>
      <button v-if="condition" @click="$emit('clearCondition')" class="btn btn-outline-primary" type="button">
        <i class="fas fa-fw fa-times"></i>
      </button>
      <button v-if="showAddButton && condition" @click="$emit('addCondition')" class="btn btn-outline-primary" type="button">
        <i class="fas fa-fw fa-plus"></i>
      </button>
    </template>
  </InputString>
</template>

<script>
import InputString from "../forms/InputString.vue";

export default {
  name: "StringFilter",
  components: { InputString },
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
      type: String,
      required: false,
    },
    showAddButton: {
      type: Boolean,
      required: false,
      default: () => false
    }
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