<template>
  <div class="card" :class="{ 'card-fullscreen': fullscreen }">
    <div ref="header" class="card-header text-center" @click="toggleCollapse">
      <h4 v-if="title">
        {{ title }}
      </h4>
      <slot name="header" />
      <IconAction
        v-if="!collapse"
        class="card-fullscreen-icon"
        :icon="fullscreen ? 'compress' : 'expand'"
        @click="toggle"
      />
    </div>
    <div
      v-if="!collapse || !collapsed"
      class="card-body"
      :style="bodyheight"
    >
      <!-- @slot Use this slot to place the card content -->
      <slot />
    </div>
  </div>
</template>

<script>
import IconAction from '../forms/IconAction.vue'

export default {

  components: {
    IconAction,
  },
  props: {
    /** If the cared should be collapsed **/
    collapse: {type: Boolean, default: false},
    /** Title that is shown on the card (optional) */
    title: String,
  },
  data: function() {
    return {
      collapsed: true,
      fullscreen: false,
    }
  },
  computed: {
    bodyheight() {
      if (this.$refs.header && this.fullscreen) {
        let header = this.$refs.header.clientHeight
        let footer = this.$refs.footer.clientHeight
        return `height: calc(100vh - ${header + footer}px)`
      }
      return ''
    },
  },
  methods: {
    toggle() {
      this.fullscreen = !this.fullscreen
    },
    toggleCollapse() {
      if (this.collapse) {
        this.collapsed = !this.collapsed
      }
    },
  },
}
</script>

<style scoped>
.card-fullscreen {
  bottom: 0;
  display: block;
  height: 100%;
  left: 0;
  overflow-y: scroll;
  position: fixed;
  right: 0;
  top: 0;
  width: 100%;
  z-index: 9999;
}

.card-fullscreen-icon {
  float: right;
  position: absolute;
  right: 0px;
  top: 0px;
}

.card-fullscreen .card-body {
  overflow-x: scroll;
}
</style>
