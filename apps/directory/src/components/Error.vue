<template>
  <div>
    <div
      v-if="error"
      class="d-flex border border-danger justify-content-between"
    >
      <span style="color: red">
        An error occurred while loading the data. Please, try a different
        search. If this error persists, please contact our helpdesk.
      </span>
      <span>
        <button type="button" class="btn" @click="clearError">X</button>
      </span>
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
import useErrorHandler from "../composables/errorHandler";

const { error, clearError } = useErrorHandler();
</script>
