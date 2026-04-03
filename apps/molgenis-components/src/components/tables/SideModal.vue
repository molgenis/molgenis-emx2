<template>
  <div class="modal fade" :class="{ show: delayedIsVisible }">
    <div class="modal-dialog modal-dialog-scrollable" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">{{ label }}</h5>
          <button
            v-if="isCloseButtonShown"
            type="button"
            class="close"
            data-dismiss="modal"
            aria-label="Close"
            @click.prevent="close"
          >
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body bg-light">
          <slot />
        </div>
        <div class="modal-footer">
          <slot name="footer" :close="close" />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onBeforeMount, onBeforeUnmount, ref, toRefs, watch } from "vue";

const props = withDefaults(
  defineProps<{
    label: string;
    isCloseButtonShown?: boolean;
    isVisible?: boolean;
  }>(),
  {
    isCloseButtonShown: true,
    isVisible: true,
  }
);

const { isVisible } = toRefs(props);

// NOTE: We need a single dom update to pass before making the component visible for css to animate the difference in styles.
let delayedIsVisible = ref(false);
delayedUpdatedIsVisible();

watch(isVisible, () => {
  delayedUpdatedIsVisible();
});

const emit = defineEmits(["onClose"]);

onBeforeMount(() => {
  if (document?.addEventListener) {
    document.addEventListener("keydown", escapeKeyHandler);
  }
});

onBeforeUnmount(() => {
  if (document?.removeEventListener) {
    document.removeEventListener("keydown", escapeKeyHandler);
  }
});

function delayedUpdatedIsVisible() {
  setTimeout(() => {
    delayedIsVisible.value = isVisible.value;
  });
}

function escapeKeyHandler(event: any) {
  if (event.key === "Escape") {
    close();
  }
}

function close() {
  delayedIsVisible.value = false;
  setTimeout(() => {
    emit("onClose");
  }, 200);
}
</script>

<style scoped>
.modal {
  display: block;
  pointer-events: none;
}

.modal-dialog {
  position: absolute;
  right: 0;
  margin-right: 0px;
}

.modal-content {
  border-right: 0;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}
.fade {
  transition: opacity 0.2s;
}

.fade .modal-dialog {
  transition: transform 0.2s, visibility 0.2s ease 0s;
}

.modal.fade.show .modal-dialog {
  transition: transform 0.2s, visibility 0.2s ease 0s;
}

.modal.fade .modal-dialog {
  transform: translate(100px, 0);
  visibility: hidden;
}

.modal.show .modal-dialog {
  transform: none;
  visibility: visible;
}
</style>

<docs>
<template>
  <SideModal
    label="SideModal - Label"
    :isVisible="showModal"
    @onClose="showModal = false"
  >
    <p>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
      tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim
      veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea
      commodo consequat. Duis aute irure dolor in reprehenderit in voluptate
      velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
      cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
      est laborum.
    </p>
    <template v-slot:footer="slot">
      <ButtonAction @click="slot.close()">Done</ButtonAction>
    </template>
  </SideModal>
  <br /><button @click="showModal = !showModal">Toggle modal</button>
</template>
<script>
export default {
  data: function () {
    return {
      showModal: false,
    };
  },
};
</script>
</docs>
