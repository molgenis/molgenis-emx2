<template>
  <Draggable
    ghost-class="border-primary"
    :group="{ name: 'g1' }"
    handle=".menu-drag-icon"
    :list="items"
    @move="$emit('change')"
  >
    <div
      v-for="(item, idx) in items"
      :key="item.key + (item.submenu ? item.submenu.length : '')"
      class="flex-nowrap"
    >
      <form class="form-inline flex-nowrap">
        <div class="col text-nowrap">
          <IconAction class="menu-drag-icon" icon="ellipsis-v" />
          <div class="d-inline">
            {{ item.label }}
          </div>
        </div>
        <InputString v-model="item.label" :default-value="item.label" />
        <InputSelect
          v-model="item.role"
          :default-value="item.role"
          :options="['Viewer', 'Editor', 'Manager']"
        />
        <InputString v-model="item.href" :default-value="item.href" />
        <IconDanger icon="trash" @click="items.splice(idx, 1)" />
      </form>
      <MenuManager v-if="item.submenu" class="ml-4" :items="item.submenu" />
    </div>
  </Draggable>
</template>

<script>
import Draggable from 'vuedraggable'
import {IconAction, IconDanger, InputSelect, InputString} from '@/components/ui/index.js'

export default {
  name: 'MenuManager',
  components: {
    Draggable,
    IconAction,
    IconDanger,
    InputSelect,
    InputString,
  },
  props: {
    items: Array,
  },
  emits: ['change'],
}
</script>
