<template>
    <div class="list-group-item list-group-item-action text-lowercase">
      <div class="form-check text-truncate">
        <input
          v-if="!hasChildren" 
          id="key-check-input"
          class="form-check-input" 
          type="checkbox" 
          v-model="isChecked" 
          @change="handleChange(keyword.name)"
        >
        <label class="form-check-label" for="key-check-input" @click="handleClick">
          {{ keyword.definition }} 
        </label>
        <i 
          v-if="hasChildren && isCollapsed" 
          class="float-right fa fa-caret-up"
          @click="handleClick"
        ></i>
        <i 
          v-if="hasChildren && !isCollapsed" 
          class="float-right fa fa-caret-down"
          @click="handleClick"
        ></i>
        <keyword-level 
          v-if="hasChildren && !isCollapsed" 
          :keywords="keyword.children"
          :handleChange="handleChange"
        ></keyword-level>
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
      handleChange: Function
    },
    data () {
      return {
        isChecked: false,
        isCollapsed: true
      }
    },
    computed: {
      hasChildren () {
        return this.keyword.children.length
      }
    },
    methods: {
      handleClick(event) {
        console.log(event.target)
        if (this.hasChildren) {
          this.isCollapsed = !this.isCollapsed
        }
      }
    }
}
</script>