<script setup lang="ts">
import type { ComponentMeta, TypeDetail } from "../utils/componentMetaTypes";
import "floating-vue/dist/style.css";

defineProps<{
  meta: ComponentMeta;
}>();

function typeDetailLines(detail: TypeDetail): string[] {
  if (detail.kind === "union") {
    return detail.options;
  }
  if (detail.kind === "array") {
    return [`element: ${detail.elementType}`];
  }
  if (detail.kind === "object") {
    return Object.entries(detail.members).map(
      ([memberName, memberType]) => `${memberName}: ${memberType}`
    );
  }
  return [];
}
</script>

<template>
  <div class="space-y-6 mt-4">
    <section v-if="meta.props.length > 0" aria-labelledby="api-props-heading">
      <h3
        id="api-props-heading"
        class="text-heading-xl text-title font-display mb-3"
      >
        Props
      </h3>
      <div class="overflow-x-auto">
        <table class="w-full text-body-md text-record-default">
          <thead>
            <tr class="border-b border-record-default text-left">
              <th class="pb-2 pr-4 font-display text-title">Name</th>
              <th class="pb-2 pr-4 font-display text-title">Type</th>
              <th class="pb-2 pr-4 font-display text-title">Default</th>
              <th class="pb-2 pr-4 font-display text-title">Required</th>
              <th class="pb-2 font-display text-title">Description</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="prop in meta.props"
              :key="prop.name"
              class="border-b border-record-default"
            >
              <td class="py-2 pr-4 font-mono text-code-output">
                {{ prop.name }}
              </td>
              <td class="py-2 pr-4 font-mono text-body-sm text-record-subtle">
                <VTooltip
                  v-if="prop.typeDetail"
                  :showTriggers="['hover', 'focus', 'touch']"
                  :distance="8"
                >
                  <button
                    class="underline decoration-dotted cursor-help focus:outline-none focus:ring-2 focus:ring-action-primary rounded-sm"
                    :aria-label="`Expand type details for ${prop.name}`"
                    type="button"
                  >
                    {{ prop.type }}
                  </button>
                  <template #popper>
                    <ul
                      class="text-body-sm font-mono space-y-0.5"
                      :aria-label="`Type details for ${prop.name}`"
                    >
                      <li
                        v-for="line in typeDetailLines(prop.typeDetail)"
                        :key="line"
                        class="whitespace-nowrap"
                      >
                        {{ line }}
                      </li>
                    </ul>
                  </template>
                </VTooltip>
                <span v-else>{{ prop.type }}</span>
              </td>
              <td class="py-2 pr-4 font-mono text-body-sm text-record-subtle">
                {{ prop.default ?? "—" }}
              </td>
              <td class="py-2 pr-4">
                <span
                  v-if="prop.required"
                  class="text-error text-body-sm font-display"
                  aria-label="required"
                >
                  required
                </span>
                <span v-else class="text-record-subtle text-body-sm">—</span>
              </td>
              <td class="py-2 text-record-subtle">
                {{ prop.description || "—" }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="meta.events.length > 0" aria-labelledby="api-events-heading">
      <h3
        id="api-events-heading"
        class="text-heading-xl text-title font-display mb-3"
      >
        Events
      </h3>
      <div class="overflow-x-auto">
        <table class="w-full text-body-md text-record-default">
          <thead>
            <tr class="border-b border-record-default text-left">
              <th class="pb-2 pr-4 font-display text-title">Name</th>
              <th class="pb-2 pr-4 font-display text-title">Payload type</th>
              <th class="pb-2 font-display text-title">Description</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="event in meta.events"
              :key="event.name"
              class="border-b border-record-default"
            >
              <td class="py-2 pr-4 font-mono text-code-output">
                {{ event.name }}
              </td>
              <td class="py-2 pr-4 font-mono text-body-sm text-record-subtle">
                {{ event.type }}
              </td>
              <td class="py-2 text-record-subtle">
                {{ event.description || "—" }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="meta.slots.length > 0" aria-labelledby="api-slots-heading">
      <h3
        id="api-slots-heading"
        class="text-heading-xl text-title font-display mb-3"
      >
        Slots
      </h3>
      <div class="overflow-x-auto">
        <table class="w-full text-body-md text-record-default">
          <thead>
            <tr class="border-b border-record-default text-left">
              <th class="pb-2 pr-4 font-display text-title">Name</th>
              <th class="pb-2 pr-4 font-display text-title">Scope type</th>
              <th class="pb-2 font-display text-title">Description</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="slot in meta.slots"
              :key="slot.name"
              class="border-b border-record-default"
            >
              <td class="py-2 pr-4 font-mono text-code-output">
                {{ slot.name }}
              </td>
              <td class="py-2 pr-4 font-mono text-body-sm text-record-subtle">
                {{ slot.type }}
              </td>
              <td class="py-2 text-record-subtle">
                {{ slot.description || "—" }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
