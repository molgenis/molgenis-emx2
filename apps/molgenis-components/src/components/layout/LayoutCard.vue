<template>
  <div class="card" :class="{ 'card-fullscreen': fullscreen }">
    <div ref="header" class="card-header text-center" @click="toggleCollapse">
      <h4 v-if="title">{{ title }}</h4>
      <slot name="header" />
      <IconAction
        v-if="!collapse"
        class="card-fullscreen-icon"
        :icon="fullscreen ? 'compress' : 'expand'"
        @click="toggle"
      />
    </div>
    <div
      v-if="!this.collapse || !collapsed"
      class="card-body"
      :style="bodyheight"
    >
      <!-- @slot Use this slot to place the card content -->
      <slot />
    </div>
  </div>
</template>

<script>
import IconAction from "../forms/IconAction.vue";

/* TODO: removed scroll lock on full screen for ssr */
export default {
  components: {
    IconAction,
  },
  props: {
    /** Title that is shown on the card (optional) */
    title: String,
    /** If the cared should be collapsed **/
    collapse: { type: Boolean, default: false },
  },
  data: function () {
    return {
      fullscreen: false,
      collapsed: true,
    };
  },
  methods: {
    toggle() {
      this.fullscreen = !this.fullscreen;
    },
    toggleCollapse() {
      if (this.collapse) {
        this.collapsed = !this.collapsed;
      }
    },
  },
  computed: {
    bodyheight() {
      if (this.$refs.header && this.fullscreen) {
        let header = this.$refs.header.clientHeight;
        let footer = this.$refs.footer.clientHeight;
        return `height: calc(100vh - ${header + footer}px)`;
      }
      return "";
    },
  },
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
<template>
  <demo-item>
    <LayoutCard title="My first card">
      Hello world
    </LayoutCard>
  </demo-item>
</template>

</docs>
