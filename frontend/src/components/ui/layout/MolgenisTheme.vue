<template>
  <div />
</template>

<script>
export default {
  props: {
    href: String,
  },
  watch: {
    href() {
      this.appendFile()
    },
  },
  mounted() {
    this.appendFile()
  },
  methods: {
    appendFile() {
      let file = document.createElement('link')
      file.rel = 'stylesheet'
      file.href = this.href
      document.head.appendChild(file)
    },
  },
}
</script>
