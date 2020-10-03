<template>
  <Draggable
    :list="items"
    handle=".menu-drag-icon"
    ghost-class="border-primary"
    :group="{ name: 'g1' }"
    @move="$emit('change')"
  >
    <div
      v-for="(item, idx) in items"
      :key="item.key + (item.submenu ? item.submenu.length : '')"
      class="flex-nowrap"
    >
      <form class="form-inline flex-nowrap ">
        <div class="col text-nowrap">
          <IconAction class="menu-drag-icon" icon="ellipsis-v" />
          <div class="d-inline">{{ item.label }}</div>
        </div>
        <InputString v-model="item.label" :defaultValue="item.label" />
        <InputSelect
          v-model="item.role"
          :defaultValue="item.role"
          :options="['Viewer', 'Editor', 'Manager']"
        />
        <InputString v-model="item.href" :defaultValue="item.href" />
        <IconDanger icon="trash" @click="items.splice(idx, 1)" />
      </form>
      <MenuManager v-if="item.submenu" :items="item.submenu" class="ml-4" />
    </div>
  </Draggable>
</template>

<script>
import Draggable from "vuedraggable";
import {
  IconAction,
  InputString,
  IconDanger,
  InputSelect
} from "@mswertz/emx2-styleguide";

export default {
  components: {
    Draggable,
    IconAction,
    InputString,
    IconDanger,
    InputSelect
  },
  props: {
    items: Array
  },
  name: "MenuManager"
};
</script>
