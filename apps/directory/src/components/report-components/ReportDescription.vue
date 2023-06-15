<template>
  <p v-if="description" class="mg-report-description">
    <b>Description: </b>{{descriptionToDisplay}}
    <span v-if="description.length > maxLength">
      <button @click="toggleDescription" class="btn btn-link p-0 pb-1">
        <span v-if="descriptionClosed">... show more <i class="fa fa-angle-down" aria-hidden="true"></i></span>
        <span v-else> show less <i class="fa fa-angle-up" aria-hidden="true"></i></span>
      </button>
    </span>
  </p>
</template>

<script>
export default {
  name: 'ReportDescription',
  props: {
    description: String,
    maxLength: Number
  },
  methods: {
    toggleDescription () {
      this.descriptionClosed = !this.descriptionClosed
    }
  },
  computed: {
    descriptionToDisplay () {
      if (this.descriptionClosed && this.maxLength < this.description.length) {
        const shortDescription = this.description.substr(0, this.maxLength)
        return shortDescription.substr(0, Math.min(shortDescription.length, shortDescription.lastIndexOf(' ')))
      } else {
        return this.description
      }
    }
  },
  data () {
    return {
      descriptionClosed: true
    }
  }
}
</script>
