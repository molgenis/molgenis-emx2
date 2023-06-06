<template>
  <Draggable
    :list="items"
    handle=".menu-drag-icon"
    ghost-class="border-primary"
    :group="{ name: 'g1' }"
    :move="$emit('change')"
    item-key="idx"
    class="flex-nowrap">
    <template #item="{ element }">
      <div>
        <form class="form-inline flex-nowrap">
          <div class="text-nowrap">
            <label v-if="idx == 0">&nbsp;</label>
            <IconAction class="menu-drag-icon" icon="ellipsis-v" />
          </div>
          <div>
            <label v-if="idx == 0">label</label>
            <InputString
              :id="'menu-label' + idx"
              v-model="element.label"
              :defaultValue="element.label" />
          </div>
          <div>
            <label v-if="idx == 0">href</label>
            <InputString
              :id="'menu-href' + idx"
              v-model="element.href"
              :defaultValue="element.href" />
          </div>
          <div>
            <label v-if="idx == 0">role</label>
            <InputSelect
              :id="'menu-role' + idx"
              v-model="element.role"
              :defaultValue="element.role"
              :options="['Viewer', 'Editor', 'Manager']" />
          </div>
          <IconDanger icon="trash" @click="items.splice(idx, 1)" />
        </form>
        <MenuManager
          v-if="element.submenu"
          :items="element.submenu"
          class="ml-4" />
      </div>
    </template>
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
  emits: ["change"],
};
</script>
