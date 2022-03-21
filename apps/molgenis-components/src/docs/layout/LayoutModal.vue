<template>
  <demo-item id="modal" label="Modal">
    <label class="font-italic">modal example</label>
    <div>
      <LayoutModal title="My first modal" @close="toggle" :show="show">
        <template v-slot:body> Here is the contents </template>
        <template v-slot:footer>
          <ButtonAction @click="toggle">Done</ButtonAction>
        </template>
      </LayoutModal>
    </div>
    <div>
      <button class="btn" @click="toggle">show</button>
    </div>
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