<template>
  <form v-on:submit.prevent="$emit('submit')"><slot /></form>
</template>

<script>
export default {
  name: "FormMolgenis",
};
</script>
