<template>
  <button type="button" class="btn btn-primary" @click="onClick">
    <slot />
    <i v-if="icon" :class="'fa fa-fw fa-' + icon" class="mr-2 ml-0"></i>
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
        window.open(this.href, "_self");
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
