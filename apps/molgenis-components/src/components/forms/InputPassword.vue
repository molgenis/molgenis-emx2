<template>
  <FormGroup :id="id" :label="label" :description="description">
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
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";

export default {
  name: "InputPassword",
  components: { FormGroup },
  extends: BaseInput,
};
</script>

<docs>
<template>
  <demo-item>
    <InputPassword
        id="password"
        v-model="value"
        label="Password"
        description="keep it secret"
    />
    <div>password = {{ value }}</div>
  </demo-item>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null,
      };
    },
  };
</script>
</docs>
