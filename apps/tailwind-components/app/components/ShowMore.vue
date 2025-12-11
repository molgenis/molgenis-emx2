<script setup lang="ts">
import { ref } from "vue";
import Button from "./Button.vue";

const showMore = ref(false);
</script>

<template>
  <div>
    <div @click="showMore = true">
      <slot v-if="!showMore" name="button">
        <Button type="text">show more</Button>
      </slot>
    </div>
    <slot v-if="showMore"></slot>
  </div>
</template>
