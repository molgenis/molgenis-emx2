<template>
  <FormGroup :id="id" :label="label" :helpText="helpText">
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
  </FormGroup>
</template>

<script>
import BaseInput from './BaseInput.vue';
import FormGroup from './FormGroup.vue';

export default {
  name: 'InputPassword',
  components: {FormGroup},
  extends: BaseInput
};
</script>

