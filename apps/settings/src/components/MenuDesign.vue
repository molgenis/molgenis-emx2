<template>
  <Draggable
    :list="items"
    handle=".menu-drag-icon"
    ghost-class="border-primary"
    :group="{ name: 'g1' }"
    :move="$emit('change')"
    item-key="key"
    class="flex-nowrap"
  >
    <template #item="{ element }">
      <div>
        <form class="form-inline flex-nowrap">
          <div class="text-nowrap">
            <label :for="'menu-drag-' + element.key" class="sr-only">
              drag item
            </label>
            <IconAction
              :id="'menu-drag-' + element.key"
              class="menu-drag-icon"
              icon="ellipsis-v"
            />
          </div>
          <div>
            <label :for="'menu-label-' + element.key" class="sr-only">
              label
            </label>
            <InputString
              :id="'menu-label-' + element.key"
              v-model="element.label"
              :defaultValue="element.label"
            />
          </div>
          <div>
            <label :for="'menu-href-' + element.key" class="sr-only">
              href
            </label>
            <InputString
              :id="'menu-href-' + element.key"
              v-model="element.href"
              :defaultValue="element.href"
            />
          </div>
          <div>
            <label :for="'menu-role-' + element.key" class="sr-only">
              role
            </label>
            <InputSelect
              :id="'menu-role-' + element.key"
              v-model="element.role"
              :defaultValue="element.role"
              :options="['Viewer', 'Editor', 'Manager']"
            />
          </div>
          <!-- <IconDanger icon="trash" @click="items.splice(key, 1)" /> -->
          <IconDanger icon="trash" @click="$emit('delete', element.key)" />
        </form>
        <MenuManager
          v-if="element.submenu"
          :items="element.submenu"
          class="ml-4"
        />
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
  emits: ["change", "delete"],
};
</script>
