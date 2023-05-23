<template>
  <span>
    <component :is="type === 'delete' ? 'IconDanger' : 'IconAction'" :icon="icon" @click="$emit(type)" />
  </span>
</template>

<script>
import IconAction from "../forms/IconAction.vue";
import IconDanger from "../forms/IconDanger.vue";

const iconMap = {
  add: "plus",
  edit: "pencil-alt",
  delete: "trash",
  clone: "copy",
};

export default {
  components: {
    IconAction,
    IconDanger,
  },
  props: {
    type: {
      type: String,
      required: true,
      validator(value) {
        return Object.keys(iconMap).includes(value);
      },
    },
  },
  computed: {
    icon() {
      return iconMap[this.type];
    },
  },
  emits: ["add", "edit", "delete", "clone"],
};
</script>

<docs>
<template>
<div>
  <label>Add</label>
  <RowButton type="add" @add="onClick('add')"/>

    <label>Edit</label>
  <RowButton type="edit" @edit="onClick('edit')"/>

    <label>Clone</label>
  <RowButton type="clone" @clone="onClick('clone')"/>

    <label>Delete</label>
  <RowButton type="delete" @delete="onClick('delete')"/>
</div>

</template>

<script>
 export default {
    methods: {
      onClick(event) {
        alert(event)
      }
    }
  };
</script>
</docs>
