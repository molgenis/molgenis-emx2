<template>
  <div>
    <div
      v-if="show"
      class="modal fade show"
      role="dialog"
      style="display: block; overflow: scroll"
      tabindex="-1"
      aria-modal="true"
    >
      <div v-if="show" class="modal-dialog modal-xl" role="document">
        <div class="modal-content">
          <div class="modal-header">
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
          <div class="modal-body bg-light">
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
  background-color: rgba(205, 205, 205, 0.5);
  max-width: 100vw;
}

.modal-body {
  /** leave room for header and foot*/
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}
</style>

<script>
export default {
  name: "LayoutModal",
  props: {
    /** When true the modal will be shown */
    show: {
      type: Boolean,
      default: true,
    },
    /** Shown as the title of the model */
    title: { type: String, default: "" },
  },
  methods: {
    close() {
      /** when the close x button is clicked */
      this.$emit("close");
    },
    escapeKeyHandler(event) {
      if (event.key === "Escape") {
        this.close();
      }
    },
  },
  beforeMount() {
    document.addEventListener("keydown", this.escapeKeyHandler);
  },
  beforeDestroy() {
    document.removeEventListener("keydown", this.escapeKeyHandler);
  },
};
</script>

<docs>
<template>
  <demo-item>
    <LayoutModal title="My first modal" @close="toggle" :show="show">
      <template v-slot:body> Here is the contents</template>
      <template v-slot:footer>
        <ButtonAction @click="toggle">Done</ButtonAction>
      </template>
    </LayoutModal>
    <button class="btn" @click="toggle">show</button>
  </demo-item>
</template>

<script>
  export default {
    data: function () {
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
</docs>
