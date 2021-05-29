<template>
  <button
    @click="$emit('click')"
    class="btn btn-sm btn-outline-primary mr-2 mb-2"
  >
    {{ label }} &nbsp;<i class="fa fa-times"></i>
  </button>
</template>
<script>
export default {
  props: {
    label: String,
  },
};
</script>
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
