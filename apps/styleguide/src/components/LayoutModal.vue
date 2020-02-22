<template>
  <div>
    <div
      v-if="show"
      class="modal fade show"
      role="dialog"
      style="display:block"
      tabindex="-1"
      aria-modal="true"
      @click="closeUnlessInDialog"
    >
      <div v-if="show" class="modal-dialog modal-xl" role="document">
        <div class="modal-content">
          <div v-if="title" class="modal-header">
            <h5 class="modal-title">{{ title }}</h5>
            <button
              type="button"
              class="close"
              data-dismiss="modal"
              aria-label="Close"
              @click="close"
            >
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div v-scroll-lock="show" class="modal-body">
            <!-- @slot contents to be shown on the modal -->
            <slot name="body" />
          </div>
          <div class="modal-footer">
            <!-- @slot contents to be shown on the modal -->
            <slot name="footer" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal {
  height: 100%;
  overflow: scroll;
}
.modal-body {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}
</style>

<script>
import VScrollLock from 'v-scroll-lock'

export default {
  directives: {
    VScrollLock
  },
  props: {
    /** Shown as the title of the model */
    title: String,
    /** When true the modal will be shown */
    show: {
      type: Boolean,
      default: true
    }
  },
  methods: {
    close () {
      /** when the close x button is clicked */
      this.$emit('close')
    },
    closeUnlessInDialog () {
      if (event.target === event.currentTarget) {
        this.$emit('close')
      }
    }
  }
}
</script>

<style scoped>
.modal {
  background: lightgray;
}
</style>

<docs>

Example

```
<template>
  <div>
    <ButtonAction @click="toggle">Toggle modal</ButtonAction>
    <LayoutModal title="My first modal" @close="toggle" :show="show">
      <template v-slot:body>
      Here is the contents
      </template>
      <template v-slot:footer>
        <ButtonAction @click="toggle">Done</ButtonAction>
      </template>
    </LayoutModal>
  </div>
</template>
<script>
export default {
  data: function() {
    return {
      show: false
    };
  },
  methods: {
    toggle() {
      this.show = !this.show;
    }
  }
};
</script>
```

</docs>
