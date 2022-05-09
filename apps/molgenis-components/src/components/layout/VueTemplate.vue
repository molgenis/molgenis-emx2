<script>
export default {
  props: {
    row: Object,
    template: String,
  },
  created () {
      this.$options.template = this.template
      this.$options.props = this.$props
  }
};
</script>

<docs>

<template>
  <VueTemplate :row="{name:'John'}" :template="template"/>
</template>
<script>
  export default {
    data() {
      return {
        template: "<h1>Hello {{row.name}}</h1>"
      }
    }
  }
</script>

</docs>
