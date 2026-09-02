<template>
  <div class="flex flex-col gap-8">
    <h4 class="text-lg">lines=3 (default)</h4>
    <ShowMore :maxLines="3">
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
      tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim
      veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea
      commodo consequat. Duis aute irure dolor in reprehenderit in voluptate
      velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
      cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
      est laborum.
    </ShowMore>

    <h4 class="text-lg">lines=1</h4>
    <ShowMore :maxLines="1">
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
      tempor incididunt ut labore et dolore magna aliqua.
    </ShowMore>

    <h4 class="text-lg">lines=5</h4>
    <ShowMore :maxLines="5">
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
      tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim
      veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea
      commodo consequat. Duis aute irure dolor in reprehenderit in voluptate
      velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
      cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
      est laborum. Sed ut perspiciatis unde omnis iste natus error sit
      voluptatem accusantium doloremque laudantium.
    </ShowMore>

    <h4 class="text-lg">Short content (no overflow)</h4>
    <ShowMore :maxLines="3">Short text that doesn't overflow.</ShowMore>

    <h4 class="text-lg">
      lines=3, content long enough to overflow at any viewport width
    </h4>
    <ShowMore :maxLines="3">{{ veryLong }}</ShowMore>

    <Markdown :source="spec" />
  </div>
</template>

<script setup lang="ts">
const veryLong = (
  "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
  "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim " +
  "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea " +
  "commodo consequat. Duis aute irure dolor in reprehenderit in voluptate " +
  "velit esse cillum dolore eu fugiat nulla pariatur. "
).repeat(20);

const spec = `
## Spec

Use this checklist when testing or modifying this component.

- [x] The control sits at the cut, on ground the mask cleared, in every theme
- [x] It appears only when something is really hidden, measured not counted
- [x] It re-measures on resize AND on a content change
- [x] Every value stays in the DOM, so in-page search still finds it
- [x] Props: \`maxLines\`, \`lineStep\` (default 5), \`hasMore\`; slots \`more\` and \`less\`
`;
</script>
