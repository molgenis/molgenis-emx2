<template>
  <InputString :id="id" :modelValue="condition" @update:modelValue="onUpdateCondition">
    <template v-slot:append>
      <button v-if="condition" @click="$emit('clearCondition')" class="btn btn-outline-primary" type="button">
        <i class="fas fa-fw fa-times"></i>
      </button>
      <button
        v-if="showAddButton && condition"
        @click="$emit('addCondition')"
        class="btn btn-outline-primary"
        type="button"
      >
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
    condition: {
      type: String,
      required: false,
    },
    showAddButton: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  methods: {
    onUpdateCondition($event) {
      this.$emit("updateCondition", $event);
    },
  },
  emits: ["updateCondition", "clearCondition", "addCondition"],
};
</script>
