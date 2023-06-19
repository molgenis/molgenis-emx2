<template>
  <dialog ref="modal">
    <article class="border border-black bg-white p-2">
      <slot />
      <div class="d-flex pt-4">
        <button class="btn btn-primary" @click="$emit('save')">Save</button>
        <button class="btn btn-dark ml-auto" @click="$emit('close')">Cancel</button>
      </div>
    </article>
  </dialog>
</template>

<script>
export default {
  props: {
    open: {
      type: Boolean,
      required: false,
      default: () => false
    }
  },
  watch: {
    open (open) {
      if (open) {
        this.$refs.modal.setAttribute('open', '')
        document.getElementsByTagName('body')[0].style.overflow = 'hidden'
      } else {
        this.$refs.modal.removeAttribute('open')
        document.getElementsByTagName('body')[0].style.removeProperty('overflow')
      }
    }
  }
}
</script>
<style scoped>
dialog[open]:not(dialog[open="false"]) {
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.5);
  border: none;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1200;
  overflow: auto;
}

article {
  position: absolute;
  top:1rem;
  z-index: 1210;
  height:auto;
  width:auto;
  border-radius: 4px;
}
</style>
