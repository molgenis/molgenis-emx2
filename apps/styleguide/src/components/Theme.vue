<template>
  <div></div>
</template>

<script>
export default {
  props: {
    href: String
  },
  methods: {
    appendFile() {
      // Removes an element from the document
      let file = document.createElement("link");
      file.rel = "stylesheet";
      file.href = this.href;
      document.head.appendChild(file);
    }
  },
  mounted() {
    this.appendFile();
  }
};
</script>
