<template>
  <div class="form-group">
    <label v-if="label !== null && label !== undefined" :id="id">{{ label }}</label>
    <input
      :id="id"
      :name="name"
      :value="value"
      @input="$emit('input', $event.target.value)"
      type="password"
      class="form-control"
      :aria-describedby="id"
      :placeholder="label"
    />
    <small v-if="isNonEmptyString(helpText)" :id="id + '-help-text'" class="form-text text-muted"
      >{{ helpText }}</small
    >
  </div>
</template>

<script>
import BaseInput from "./BaseInput.vue";

export default {
  name: "InputPassword",
  extends: BaseInput,
};
</script>

