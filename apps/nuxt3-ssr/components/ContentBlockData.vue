<script setup>
import ContentBlock from "./ContentBlock.vue";
import List from "./List.vue";
import ListItem from "./ListItem.vue";
import ListTree from "./ListTree.vue";
import ListTreeItem from "./ListTreeItem.vue";

defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
});
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <div class="grid gap-12.5">
      <List title="Data categories" columnCount="2">
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
      </List>
      <List title="Sample categories" columnCount="3">
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
      </List>
      <List title="Areas of informations" columnCount="2">
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
        <ListItem />
      </List>

      <ListTree title="Areas of informations tree view">
        <ListTreeItem title="Administrave information" count="6" tooltip="todo">
          <ListTreeItem title="Biochemistry" />
          <ListTreeItem title="Biochemistry" />
          <ListTreeItem title="Biochemistry" />
          <ListTreeItem title="Biochemistry" />
        </ListTreeItem>
        <ListTreeItem title="Administrave information" count="6" tooltip="todo">
          <ListTreeItem title="Biochemistry" />
          <ListTreeItem title="Biochemistry" />
          <ListTreeItem title="Biochemistry" />
          <ListTreeItem title="Biochemistry" />
        </ListTreeItem>
      </ListTree>
    </div>
  </ContentBlock>
</template>
