<template>
  <demo-item id="button-action" label="Button Action">
    <div>
      <ButtonAction v-on:click="alert('hello')">Action</ButtonAction>
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