<template>
  <button class="btn text-danger" style="width: 40px" @click="onClick">
    <i :class="'fa fa-'+icon"></i>
  </button>
</template>

<script>
/** Button that is shown as a icon */
export default {
  props: {
    icon: String
  },
  methods: {
    onClick () {
      /** emitted on click */
      this.$emit('click')
    }
  }
}
</script>

<docs>
Example

```jsx
<IconDanger icon="times"/>
```

</docs>
