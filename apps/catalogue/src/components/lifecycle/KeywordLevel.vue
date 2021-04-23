<template>
  <div class="list-group text-nowrap ">
    <keyword-tree-item v-for="(keyword, index) in keywords" :key=index :keyword="keyword" />
  </div>
</template>

<script>
import KeywordTreeItem from './KeywordTreeItem.vue'

export default {
    name: 'KeywordLevel',
    components: { KeywordTreeItem },
    props: {
      keywords: Array,
    }
}
</script>