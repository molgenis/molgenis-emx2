<script setup>
</script>

<template>
  <div class="flex">
    <div class="m-4 flex-1">
      <div>
        <InputString
          :id="id"
          v-model="fieldValue"
          :has-error="hasError"
          :required="required"
        />
      </div>
    </div>
  </div>
</template>