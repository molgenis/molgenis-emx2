<template>
  <div class="card" :class="{ 'card-fullscreen': fullscreen }">
    <div ref="header" class="card-header text-center">
      <h4 v-if="title">{{ title }}</h4>
      <slot name="header" />
      <IconAction
        class="card-fullscreen-icon"
        :icon="fullscreen ? 'compress' : 'expand'"
        @click="toggle"
      />
    </div>
    <div v-scroll-lock="fullscreen" class="card-body" :style="bodyheight">
      <!-- @slot Use this slot to place the card content -->
      <slot />
    </div>
  </div>
</template>

<script>
import IconAction from "../forms/IconAction";
import Vue from "vue";
import VScrollLock from "v-scroll-lock";

Vue.use(VScrollLock);

export default {
  directives: {
    VScrollLock
  },
  components: {
    IconAction
  },
  props: {
    /** Title that is shown on the card (optional) */
    title: String
  },
  data: function() {
    return {
      fullscreen: false
    };
  },
  methods: {
    toggle() {
      this.fullscreen = !this.fullscreen;
    }
  },
  computed: {
    bodyheight() {
      if (this.$refs.header && this.fullscreen) {
        let header = this.$refs.header.clientHeight;
        let footer = this.$refs.footer.clientHeight;
        return `height: calc(100vh - ${header + footer}px)`;
      }
      return "";
    }
  }
};
</script>

<style scoped>
.card-fullscreen {
  display: block;
  z-index: 9999;
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  right: 0;
  left: 0;
  bottom: 0;
  overflow-y: scroll;
}

.card-fullscreen-icon {
  float: right;
  position: absolute;
  top: 0px;
  right: 0px;
}

.card-fullscreen .card-body {
  overflow-x: scroll;
}
</style>

<docs>

Example

```jsx
<LayoutCard title="My first card">
  Hello world
  <ButtonAction>Hello</ButtonAction>
</LayoutCard>
```

</docs>
