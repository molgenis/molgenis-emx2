<template>
  <div class="d-flex justify-content-center">
    <div
      v-if="error"
      class="border border-danger justify-content-between bd-highligh p-2 align-middle"
    >
      <span v-if="typeof error === `string`" style="color: red">
        {{ error }}
      </span>
      <span v-else style="color: red">
        An error occurred while loading the data. Please, try a different
        search. If this error persists, please contact our helpdesk.
      </span>
      <span>
        <button class="btn" type="button" @click="clearError">
          <span class="fa-solid fa-xmark" />
        </button>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import useErrorHandler from "../composables/errorHandler";

const { error, clearError } = useErrorHandler();
</script>
