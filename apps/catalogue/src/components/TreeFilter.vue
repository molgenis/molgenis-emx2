<template>
  <li v-if="topic.match">
    <button class="btn text-primary" @click="click">
      <i
        :class="{
          'fa fa-folder': topic.childTopics.length > 0 && topic.collapsed,
          'fa fa-folder-open': topic.childTopics.length > 0 && !topic.collapsed,
          'fa fa-file': topic.childTopics.length == 0,
        }"
      >
      </i>
    </button>
    <div class="form-check form-check-inline">
      <input
        class="form-check-input"
        type="checkbox"
        v-model="topic.checked"
        @click="
          topic.checked = !topic.checked;
          selectChildren(topic, !topic.checked);
          $emit('changed');
        "
        :key="JSON.stringify(topic)"
      />
    </div>
    {{ topic.name }}

    <ul class="fa-ul" v-if="topic.childTopics && !topic.collapsed">
      <tree-node
        v-for="subtopic in topic.childTopics"
        :topic="subtopic"
        :key="JSON.stringify(topic)"
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
    selectChildren(topic, checked) {
      topic.checked = checked;
      if (topic.childTopics) {
        topic.childTopics.forEach((t) => {
          t.checked = checked;
          this.selectChildren(t, checked);
        });
      }
    },
    click() {
      if (this.topic.childTopics) {
        this.clickRecursiveIfOne(this.topic);
      }
    },
    clickRecursiveIfOne(topic) {
      topic.collapsed = !topic.collapsed;
      if (
        topic.childTopics &&
        topic.childTopics.filter((c) => c.match).length == 1
      ) {
        this.clickRecursiveIfOne(topic.childTopics[0]);
        topic.childTopics[0].collapsed = !topic.childTopics[0].collapsed;
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
