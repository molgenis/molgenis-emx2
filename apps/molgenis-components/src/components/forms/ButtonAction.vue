<template>
  <button
    type="button"
    class="btn btn-primary"
    :disabled="disabled"
    @click.prevent="onClick">
    <slot />
    <i v-if="icon" :class="'fa fa-fw fa-' + icon" class="mr-2 ml-0"></i>
  </button>
</template>

<script>
export default {
  name: "ButtonAction",
  props: {
    href: { type: String, required: false },
    icon: { type: String, required: false },
    disabled: { type: Boolean, required: false },
  },
  methods: {
    onClick() {
      if (window && window.open && this.href) {
        window.open(this.href, "_self");
      } else {
        /** emitted on click */
        this.$emit("click");
      }
    },
  },
  emits: ["click"],
};
</script>

<docs>
<template>
  <demo-item id="button-action" label="Button Action">
    <div>
      <ButtonAction v-on:click="alert('hello')">Action</ButtonAction>
    </div>
  </demo-item>
</template>
<script>
  export default {
    methods: {
      alert(text) {
        alert(text);
      },
    },
  };
</script>
</docs>
