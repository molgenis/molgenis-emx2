<template>
    <div class="list-group-item list-group-item-action text-lowercase">
      <div class="form-check text-truncate">
        <input class="form-check-input" type="checkbox" value="" id="key-check-input">
        <label class="form-check-label" for="key-check-input">
          {{ keyword.definition }} 
        </label>
        <i 
          v-if="hasChildren" 
          class="float-right fa fa-caret-up"
          @click="isCollapsed = !isCollapsed"
        ></i>
        <keyword-level v-if="hasChildren && !isCollapsed" :keywords="keyword.children"></keyword-level>
      </div>
    </div>
</template>

<script>
export default {
    name: 'KeywordTreeItem',
    components: {
      KeywordLevel: () => import('./KeywordLevel.vue')
    },
    props: {
      keyword: Object,
    },
    data () {
      return {
        isCollapsed: true
      }
    },
    computed: {
      hasChildren () {
        return this.keyword.children.length
      }
    }
}
</script>