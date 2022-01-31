<template>
    <div :id="id" class="mb-5 pt-3 border-top">
    <h3>{{ label }}</h3>

    <slot></slot>
    </div>
</template>

<script>
export default {
  name: "DemoItem",
  props: {
      id: {
        type: String,
        required: true
      },
      label: {
        type: String,       
      }
  }
}
</script>

<style>

</style>