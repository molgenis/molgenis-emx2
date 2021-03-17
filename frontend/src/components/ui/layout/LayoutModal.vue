<template>
    <div>
        <div
            v-if="show"
            aria-modal="true"
            class="modal fade show"
            role="dialog"
            style="display: block"
            tabindex="-1"
            @click="closeUnlessInDialog"
        >
            <div v-if="show" class="modal-dialog modal-xl" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            {{ title }}
                        </h5>
                        <button
                            aria-label="Close"
                            class="close"
                            data-dismiss="modal"
                            type="button"
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

<script>
import VScrollLock from "v-scroll-lock";

export default {
  directives: {
    VScrollLock,
  },
  props: {
    /** Shown as the title of the model */
    title: { type: String, default: "" },
    /** When true the modal will be shown */
    show: {
      type: Boolean,
      default: true,
    },
  },
  methods: {
    close() {
      /** when the close x button is clicked */
      this.$emit("close");
    },
    closeUnlessInDialog() {
      if (event.target === event.currentTarget) {
        this.$emit("close");
      }
    },
  },
};
</script>

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
