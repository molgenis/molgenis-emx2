<template>
  <li v-if="topic.match">
    <div @click="click">
      <span class="fa-li"
        ><i
          :class="{
            'fa fa-folder': topic.childTopics && topic.collapsed,
            'fa fa-folder-open': topic.childTopics && !topic.collapsed,
            'fa fa-file': !topic.childTopics,
          }"
        ></i
      ></span>
    </div>
    {{ topic.name }}
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
        this.topic.collapsed = !this.topic.collapsed;
        this.$forceUpdate();
      } else {
        this.select(this.topic);
      }
    },
    select(topic) {
      this.$emit("select", topic);
    },
  },
  created() {
    if (this.topic.collapsed == undefined) {
      this.topic.collapsed = true;
    }
  },
};
</script>
