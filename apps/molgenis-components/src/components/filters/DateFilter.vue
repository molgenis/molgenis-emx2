<template>
  <InputRangeDate :id="id" :value="condition" @input="onUpdateCondition">
    <template v-slot:append>
      <button
        v-if="condition"
        @click="$emit('clearCondition')"
        class="btn btn-outline-primary"
        type="button"
      >
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
  </InputRangeDate>
</template>

<script>
import InputRangeDate from "../forms/InputRangeDate.vue";

export default {
  name: "DateFilter",
  components: { InputRangeDate },
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
