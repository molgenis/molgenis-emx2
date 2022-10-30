<template>
  <InputBoolean
    :id="id"
    v-model="condition"
    @update:modelValue="onUpdateCondition"
    @clearCondition="$emit('clearCondition')"
  />
</template>

<script>
import InputBoolean from "../forms/InputBoolean.vue";

export default {
  name: "BooleanFilter",
  components: { InputBoolean },
  props: {
    id: {
      type: String,
      required: true,
    },
    condition: {
      type: Boolean,
      required: false,
    },
  },
  methods: {
    onUpdateCondition($event) {
      console.log(this.condition)
      if ($event === null) {
        this.$emit("clearCondition", $event);
      } else {
        this.$emit("updateCondition", $event);
      }
    },
  },
  emits:['updateCondition','clearCondition','addCondition']
};
</script>