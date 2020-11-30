<template>
  <li v-if="topic.match">
    <button class="btn text-primary" @click="click">
      <i
        :class="{
          'fa fa-2x fa-folder': topic.childTopics && topic.collapsed,
          'fa fa-2x fa-folder-open': topic.childTopics && !topic.collapsed,
          'fa fa-2x fa-file': !topic.childTopics,
        }"
      >
      </i>

      {{ topic.name }}
    </button>
    <ul class="fa-ul" v-if="topic.childTopics && !topic.collapsed">
      <tree-node
        v-for="subtopic in topic.childTopics"
        :topic="subtopic"
        :key="subtopic.name + subtopic.match + subtopic.collapsed"
        @select="select"
      />
    </ul>
  </li>
</template>
<script>
export default {
  name: "tree-node",
  props: {
    topic: Object,
  },
  methods: {
    click() {
      if (this.topic.childTopics) {
        this.clickRecursiveIfOne(this.topic);
        this.$forceUpdate();
      } else {
        this.select(this.topic);
      }
    },
    select(topic) {
      this.$emit("select", topic);
    },
    clickRecursiveIfOne(topic) {
      topic.collapsed = !topic.collapsed;
      if (
        topic.childTopics &&
        topic.childTopics.filter((c) => c.match).length == 1
      ) {
        this.clickRecursiveIfOne(topic.childTopics[0]);
        topic.childTopics[0].collapsed = !topic.childTopics[0].collapsed;
      } else {
        this.select(topic);
      }
    },
  },
  created() {
    if (this.topic.collapsed == undefined) {
      this.topic.collapsed = true;
    }
  },
};
</script>
