<template>
  <li v-if="topic.match">
    <button class="btn text-primary" @click="click">
      <i
        :class="{
          'fa fa-folder': topic.childTopics.length > 0 && topic.collapsed,
          'fa fa-folder-open': topic.childTopics.length > 0 && !topic.collapsed,
          'fa fa-file': topic.childTopics.length == 0,
        }"
      />
    </button>
    <div class="form-check form-check-inline">
      <input
        :key="JSON.stringify(topic)"
        v-model="topic.checked"
        class="form-check-input"
        type="checkbox"
        @click="
          topic.checked = !topic.checked;
          selectChildren(topic, !topic.checked);
          $emit('changed');
        "
      >
    </div>
    {{ topic.name }}

    <ul v-if="topic.childTopics && !topic.collapsed" class="fa-ul">
      <tree-node
        v-for="subtopic in topic.childTopics"
        :key="JSON.stringify(topic)"
        :topic="subtopic"
      />
    </ul>
  </li>
</template>

<script>
export default {
  name: 'TreeNode',
  props: {
    topic: Object,
  },
  emits: ['changed'],
  created() {
    if (this.topic.collapsed == undefined) {
      this.topic.collapsed = true
    }
  },
  methods: {
    click() {
      if (this.topic.childTopics) {
        this.clickRecursiveIfOne(this.topic)
      }
    },
    clickRecursiveIfOne(topic) {
      topic.collapsed = !topic.collapsed
      if (
        topic.childTopics &&
        topic.childTopics.filter((c) => c.match).length == 1
      ) {
        this.clickRecursiveIfOne(topic.childTopics[0])
        topic.childTopics[0].collapsed = !topic.childTopics[0].collapsed
      }
    },
    selectChildren(topic, checked) {
      topic.checked = checked
      if (topic.childTopics) {
        topic.childTopics.forEach((t) => {
          t.checked = checked
          this.selectChildren(t, checked)
        })
      }
    },
  },
}
</script>
