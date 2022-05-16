<template>
  <button type="button" class="btn btn-primary" @click="onClick">
    <slot />
    <i v-if="icon" :class="'fa fa-fw fa-' + icon" class="mr-2 ml-0"></i>
  </button>
</template>

<script>
export default {
  name: "ButtonAction",
  props: {
    href: String,
    icon: String,
  },
  methods: {
    onClick() {
      if (window && this.href) {
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
