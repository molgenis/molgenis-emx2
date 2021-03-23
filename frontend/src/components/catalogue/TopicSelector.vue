<template>
  <div>
    <div
      v-for="topic in topics.filter((t) => t.match)"
      :key="topic.name + timestamp"
    >
      <IconAction
        v-if="topic.childTopics.length == 0"
        icon="file"
        @click="select(topic)"
      />
      <IconAction
        v-else-if="topic.expand"
        icon="folder-open"
        @click="toggle(topic)"
      />
      <IconAction v-else icon="folder" @click="toggle(topic)" />
      <span :class="{ 'bg-primary text-white': topic.name == selected }">{{
        topic.name
      }}</span>
      <div
        v-if="topic.childTopics.length > 0 && topic.expand"
        :key="topic.name"
        class="ml-5"
      >
        <topic-selector
          :selected="selected"
          :topics="topic.childTopics"
          @deselect="deselect"
          @select="select"
        />
      </div>
    </div>
  </div>
</template>

<script>
import {IconAction} from '@/components/ui/.index.js'

export default {
  name: 'TopicSelector',
  components: {
    IconAction,
    'tree-node': this,
  },
  props: {
    topics: Array, // array of Topic
    selected: String,
  },
  data() {
    return {
      timestamp: null,
    }
  },
  methods: {
    toggle(topic) {
      topic.expand = !topic.expand
      this.timestamp = Date.now()
    },
    select(topic) {
      if (this.selected == topic.name) {
        this.deselect()
      } else {
        this.$emit('select', topic)
      }
    },
    deselect() {
      this.$emit('deselect')
    },
  },
}
</script>
