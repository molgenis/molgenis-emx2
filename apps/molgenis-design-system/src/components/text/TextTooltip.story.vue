<template>
  <Story title="Text/Tooltip" :layout="{ type: 'grid', width: '65%' }">
    <Variant title="Default">
      <p>From the <TextTooltip description="Web Accessibility Initiative">WAI</TextTooltip>; accessed 2023-12-05</p>
      <blockquote class="border-l-2 p-2 mb-2">
        A tooltip is a popup that displays information related to an element when the element receives keyboard focus or the mouse hovers over it. It typically appears after a small delay and disappears when <kbd>Escape</kbd> is pressed or on mouse out.
      </blockquote>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import TextTooltip from "./TextTooltip.vue";
</script>

<docs lang="md">
## Text Tooltip

The `<TextTooltip>` component displays additional text when a text element is
hovered. This may be useful for situtations where you would like to display the
full name for an abbreviation or a short definition for a word. However,
tooltips aren't designed to have other elements besides text.  They should not
contain links, images, buttons, or other similar elements.

It is recommended to use this sparingly as any content that is moved to a tooltip
won't be visisble to the user. If the tooltip provides important content, then it
should be added to the rest of the content.

### Usage

The following attributes are available.

| Name            | Type    | Required | Description                          | Default    |
| :-------------- | :------ | :------- | :------------------------------------| :--------- |
| Description | `string` | `true` | additional text to display in the tooltip | ---        |

### Slots

Use the `<template>` element and the following slot name(s) to add additional context to the component.

| Name      | Vue Slot | Description                                         |
| :---------| :------- | :-------------------------------------------------- |
| Default\* | ---      | the text that will tooltip is linked with           |

 \*Note: wrapping text in a named template is not needed here.
 
### Examples

```js
<script setup lang="ts">
import TextTooltip from "molgenis-design-system";
</script>

<template>
  <p>This is my <TextTooltip description="some text">tooltip</TextTooltip>.</p>
</template>
```
 
</docs>