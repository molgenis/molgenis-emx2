<template>
  <demo-item id="input-password" label="Input Password">
    <div>
      <InputPassword
        id="password"
        v-model="value"
        label="Password"
        description="keep it secret"
      />
      <div>password = {{value}}</div>
    </div>
  </demo-item>
</template>
<script>
import InputPassword from "../../components/forms/InputPassword.vue";
export default {
    data: function () {
        return {
            value: null,
        };
    },
    components: { InputPassword }
};
</script>