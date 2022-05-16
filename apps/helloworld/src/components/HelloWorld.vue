<template>
  <div>
    <h1>Hello World</h1>
    <ButtonAction @click="plusOne">{{ count }}</ButtonAction>
  </div>
</template>

<script>
import { ButtonAction } from "molgenis-components";

export default {
  components: {
    ButtonAction,
  },
  data() {
    return {
      count: 0,
    };
  },
  methods: {
    plusOne() {
      this.count++;
    },
  },
};
</script>
