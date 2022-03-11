<template>
  <demo-item id="input-string" label="Input String">
    <div>
      <InputString
        v-model="value"
        label="My string input label"
        description="Some help needed?"
      />
      <div>You typed: {{ JSON.stringify(value) }}</div>
    </div>
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