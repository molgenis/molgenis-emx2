<template>
  <demo-item id="button-outline" label="Button Outline">
    <div>
      <ButtonOutline v-on:click="alert('cancel clicked')">Cancel</ButtonOutline>
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