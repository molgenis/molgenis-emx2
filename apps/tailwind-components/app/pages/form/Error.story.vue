<template>
  <FormError
    message="3 fields require your attention before you can save this cohort"
    @error-prev="errorPrev"
    @error-next="errorNext"
  />
</template>

<script setup lang="ts">
function errorPrev() {
  alert("Previous error");
}

function errorNext() {
  alert("Next error");
}
</script>
