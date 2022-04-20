<template>
  <demo-item id="array-input" label="Array Input">
    <div>
      <ArrayInput
        id="array-string-1"
        type="STRING_ARRAY"
        v-model="value"
      ></ArrayInput>
    </div>
    <div>
      {{ value }}
    </div>
  </demo-item>
</template>
<script>
export default {
  methods: {
    alert(text) {
      alert(text);
    },
  },
  data() {
    return {
      value: ["blaat"],
    };
  },
};
</script>