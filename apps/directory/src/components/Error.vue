<template>
  <div>
    <div v-if="error" style="color: red">
      An Error has occurred loading the Directory app, see the dev console for
      more information.
      <button type="button" class="btn" @click="clearError">X</button>
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
import useErrorHandler from "../composables/errorHandler";

const { error, clearError } = useErrorHandler();
</script>
