<template>
  <demo-item id="button-alt" label="Button Alt">
    <div>
      <ButtonAlt v-on:click="alert('cancel clicked')">Cancel</ButtonAlt>
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
};
</script>