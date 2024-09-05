<template>
  <FormGroup
    :id="id"
    :label="label"
    :description="description"
    :errorMessage="errorMessage"
  >
    <input
      :id="id"
      :name="name"
      :value="modelValue"
      @input="updateModelValue($event.target.value)"
      type="password"
      class="form-control"
      :aria-describedby="id"
      :placeholder="label"
      :class="{ 'is-invalid': errorMessage }"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";

export default {
  name: "InputPassword",
  components: { FormGroup },
  extends: BaseInput,
  methods: {
    updateModelValue: function (value) {
      this.$emit("update:modelValue", value == "" ? null : value);
    },
  },
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
