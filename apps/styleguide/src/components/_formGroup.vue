/** internal component that will not be shown in style guide */

<template>
  <div class="form-group">
    <label v-if="label" :for="id">
      <b>{{label}}</b>
      <span v-if="nullable">(optional)</span>
    </label>
    <slot />
    <div v-if="error" class="text-danger">{{error}}</div>
    <div v-else-if="help" :id="id + 'Help'" class="form-text text-muted">{{help}}</div>
  </div>
</template>

<script>
export default {
  props: {
    /** id for which this is the group */
    id: String,
    /** value to be shown as input */
    placeholder: String,
    /** label to be shown next to the input */
    label: String,
    /** optional help string shown below */
    help: String,
    /** if optional */
    nullable: Boolean,
    /** String with error state */
    error: String
  }
}
</script>

<style scoped>
.form-control.is-invalid {
  background-image: none;
}
</style>
