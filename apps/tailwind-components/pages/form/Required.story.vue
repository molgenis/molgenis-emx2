<template>
  <FormRequired
    message="3/20 required fields left"
    @required-prev="requiredPrev"
    @required-next="requiredNext"
  />
</template>

<script setup lang="ts">
function requiredPrev() {
  alert("Previous required field");
}

function requiredNext() {
  alert("Next required field");
}
</script>
