<template>
  <button @click="$emit('click')" class="btn btn-outline-primary mr-2 mb-2">
    {{ label }} &nbsp;<i :class="'fa fa-times'"></i>
  </button>
</template>
<script>
export default {
  props: {
    label: String
  }
}
</script>
<style scoped>
.btn i {
  visibility: hidden;
}

.btn:hover i {
  visibility: visible;
}
</style>
<docs>
    Example
    ```
    <template>
        <FilterWell label="foo = bar" @click="showAlert"/>
    </template>
    <script>
        export default {
            methods: {
                showAlert() {
                    alert('clicked')
                }
            }
        }
    </script>
    ```
</docs>
