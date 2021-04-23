<template>
  <keyword-level 
    :keywords="Object.values(keywordTree)"
    @change-keyword-check="handleKeywordSelectionChange"
    :handleChange="handleKeywordSelectionChange"
  >
</keyword-level>
</template>

<script>
import KeywordLevel from './KeywordLevel.vue'

export default {
  name: 'KeywordTree',
  components: { KeywordLevel },
  props: {
    keywordTree: Array,
    selectedKeywords: Array
  },
  methods: {
    handleKeywordSelectionChange (name) {
        if(this.selectedKeywords.includes(name)) {
          this.selectedKeywords.splice(this.selectedKeywords.indexOf(name), 1)
        } else {
          this.selectedKeywords.push(name)
        }
    }
  }
}
</script>