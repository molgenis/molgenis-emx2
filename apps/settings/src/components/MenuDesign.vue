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
      <form class="form-inline flex-nowrap">
        <div class="text-nowrap">
          <label v-if="idx == 0">&nbsp;</label>
          <IconAction class="menu-drag-icon" icon="ellipsis-v" />
        </div>
        <div>
          <label v-if="idx == 0">label</label>
          <InputString :id="'menu-label' + idx" v-model="item.label" :defaultValue="item.label" />
        </div>
        <div>
          <label v-if="idx == 0">href</label>
          <InputString :id="'menu-href' + idx" v-model="item.href" :defaultValue="item.href" />
        </div>
        <div>
          <label v-if="idx == 0">role</label>
          <InputSelect
            :id="'menu-role' + idx"
            v-model="item.role"
            :defaultValue="item.role"
            :options="['Viewer', 'Editor', 'Manager']"
          />
        </div>
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
  IconDanger,
  InputSelect,
  InputString,
} from "molgenis-components";

export default {
  components: {
    Draggable,
    IconAction,
    InputString,
    IconDanger,
    InputSelect,
  },
  props: {
    items: Array,
  },
  name: "MenuManager",
};
</script>
