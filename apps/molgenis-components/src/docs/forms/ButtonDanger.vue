<template>
  <demo-item id="button-danger" label="Button Danger">
    <div>
      <ButtonDanger v-on:click="alert('danger!')">danger</ButtonDanger>
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