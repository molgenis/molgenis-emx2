<template>
  <button type="button" class="btn btn-primary" @click="onClick">
    <i v-if="icon" :class="icon"></i>
    <slot />
  </button>
</template>

<script>
/** Button that is shown as a primary action */
export default {
  props: {
    href: String,
    icon: String,
  },
  methods: {
    onClick() {
      if (this.href) {
        window.open(this.href);
      } else {
        /** emitted on click */
        this.$emit("click");
      }
    },
  },
};
</script>

<docs>
Example

```
<template>
  <ButtonAction v-on:click="alert('hello');">Action</ButtonAction>
</template>
<script>
  export default {
    methods: {
      alert(text) {
        alert(text);
      }
    }
  }
</script>
```

</docs>
