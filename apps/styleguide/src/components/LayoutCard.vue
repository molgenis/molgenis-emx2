<template>
  <div class="card" :class="{'card-fullscreen': fullscreen}">
    <div ref="header" class="card-header text-center">
      <h4 v-if="title">{{title}}</h4>
      <slot name="header" />
      <IconAction
        class="card-fullscreen-icon"
        :icon="fullscreen? 'compress' : 'expand'"
        @click="fullscreen = !fullscreen"
      />
    </div>
    <div v-scroll-lock="fullscreen" class="card-body" :style="bodyheight">
      <!-- @slot Use this slot to place the card content -->
      <slot />
    </div>
    <div ref="footer" class="card-footer">Created by MOLGENIS.</div>
  </div>
</template>

<script>
import IconAction from './IconAction'
import VScrollLock from 'v-scroll-lock'

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
  data: function () {
    return {
      fullscreen: false
    }
  },
  computed: {
    bodyheight () {
      if (this.fullscreen) {
        let header = this.$refs.header.clientHeight
        let footer = this.$refs.footer.clientHeight
        return `height: calc(100vh - ${header + footer}px)`
      }
      return ''
    }
    // version () {
    //   return this.$store.state.version
    // }
  }
}
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
