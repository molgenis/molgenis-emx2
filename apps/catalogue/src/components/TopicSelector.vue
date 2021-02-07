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
        class="ml-5"
        v-if="topic.childTopics.length > 0 && topic.expand"
        :key="topic.name"
      >
        <topic-selector
          :topics="topic.childTopics"
          :selected="selected"
          @select="select"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { IconAction } from "@mswertz/emx2-styleguide";

export default {
  name: "topic-selector",
  components: {
    IconAction,
    "tree-node": this,
  },
  props: {
    topics: Array, //array of Topic
    selected: String,
  },
  data() {
    return {
      timestamp: null,
    };
  },
  methods: {
    toggle(topic) {
      topic.expand = !topic.expand;
      this.timestamp = Date.now();
    },
    select(topic) {
      this.$emit("select", topic);
    },
  },
};
</script>
