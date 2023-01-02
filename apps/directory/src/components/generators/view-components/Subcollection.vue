<template>
  <div class="mb-3" :class="indentationLevel">
    <b>
      <router-link :to="'/collection/' + collection.id">
        {{ collection.name }}
      </router-link>
    </b>
    <view-generator :viewmodel="collection.viewmodel" />
  </div>
</template>

<script>
export default {
  name: 'Subcollection',
  components: {
    // required because of recursion, else Vue will give component not found error
    ViewGenerator: () => import('../ViewGenerator.vue')
  },
  props: {
    collection: {
      type: Object,
      required: true
    },
    level: {
      type: Number,
      default: () => 1
    }
  },
  computed: {
    indentationLevel () {
      return `ml-${this.level}`
    }
  }
}
</script>
