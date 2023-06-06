<template>
  <InputRangeDateTime
    :id="id"
    :modelValue="condition"
    @update:modelValue="onUpdateCondition">
    <template v-slot:append>
      <button
        v-if="condition"
        @click="$emit('clearCondition')"
        class="btn btn-outline-primary"
        type="button">
        <i class="fas fa-fw fa-times"></i>
      </button>
      <button
        v-if="showAddButton && condition"
        @click="$emit('addCondition')"
        class="btn btn-outline-primary"
        type="button">
        <i class="fas fa-fw fa-plus"></i>
      </button>
    </template>
  </InputRangeDateTime>
</template>

<script>
import InputRangeDateTime from "../forms/InputRangeDateTime.vue";

export default {
  name: "DateTimeFilter",
  components: { InputRangeDateTime },
  props: {
    id: {
      type: String,
      required: true,
    },
    condition: {
      type: Array,
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
};
</script>
