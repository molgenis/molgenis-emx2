<template>
  <div class="flex flex-col gap-8">
    <h4 class="text-lg">lines=3 (default)</h4>
    <ShowMore>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
      tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim
      veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea
      commodo consequat. Duis aute irure dolor in reprehenderit in voluptate
      velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
      cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
      est laborum.
    </ShowMore>

    <h4 class="text-lg">lines=1</h4>
    <ShowMore :lines="1">
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
      tempor incididunt ut labore et dolore magna aliqua.
    </ShowMore>

    <h4 class="text-lg">lines=5</h4>
    <ShowMore :lines="5">
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
    <ShowMore :lines="3"> Short text that doesn't overflow. </ShowMore>

    <Markdown :source="spec" />
  </div>
</template>

<script setup lang="ts">
const spec = `
## Spec

Use this checklist when testing or modifying this component.

- [x] No flicker after SSR hydration
- [x] Text fades out at bottom (works on all theme colors)
- [x] "show more" button appears below text, left-aligned, small
- [x] Button only appears when text is too long
- [x] Button adjusts when window is resized
- [x] Props: \`lines\` (default 3), \`showLabels\`
`;
</script>
