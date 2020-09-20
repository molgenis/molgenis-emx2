<template>
  <Draggable
    :list="items"
    handle=".menu-drag-icon"
    ghost-class="border-primary"
    :group="{ name: 'g1' }"
  >
    <div
      v-for="(item, idx) in items"
      :key="item.key + item.submenu.length"
      class="flex-nowrap"
    >
      <form class="form-inline flex-nowrap ">
        <div class="col text-nowrap">
          <IconAction class="menu-drag-icon" icon="ellipsis-v" />
          <div class="d-inline">{{ item.label }}</div>
        </div>
        <InputString v-model="item.label" :defaultValue="item.label" />
        <InputString v-model="item.href" :defaultValue="item.href" />
        <IconDanger icon="trash" @click="items.splice(idx, 1)" />
      </form>
      <MenuManager v-if="item.submenu" :items="item.submenu" class="ml-4" />
    </div>
  </Draggable>
</template>

<script>
import Draggable from "vuedraggable";
import { IconAction, InputString, IconDanger } from "@mswertz/emx2-styleguide";

export default {
  components: {
    Draggable,
    IconAction,
    InputString,
    IconDanger
  },
  props: {
    items: Array
  },
  name: "MenuManager",
  created() {
    this.items.forEach(item => {
      //give random keys so we can monitor moves
      item.key = Math.random()
        .toString(36)
        .substring(7);
      //give empty submenu so we can drag-nest
      if (item.submenu == undefined) {
        item.submenu = [];
      }
    });
  }
};
</script>
