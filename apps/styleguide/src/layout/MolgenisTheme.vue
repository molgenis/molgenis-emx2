<template>
  <div></div>
</template>

<script>
export default {
  props: {
    href: String,
  },
  methods: {
    appendFile() {
      let file = document.createElement("link");
      file.rel = "stylesheet";
      file.href = this.href;
      document.head.appendChild(file);
    },
  },
  mounted() {
    this.appendFile();
  },
  watch: {
    href() {
      this.appendFile();
    },
  },
};
</script>
