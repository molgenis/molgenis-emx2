# UnorderedList

The `<UnorderedList />` component is primarily used in the `<PageFooter />` component to display links to other pages (both internally and externally). The list may be rendered horizontally (ideal for the links to legal pages) or vertically (site maps).

## Props

<!-- @vuese:UnorderedList:props:start -->

| Name       | Description                                                                   | Type                            | Required | Default  |
| ---------- | ----------------------------------------------------------------------------- | ------------------------------- | -------- | -------- |
| listLayout | determine if the list should be rendered vertically (default) or horizontally | `'vertical' / 'horizontal'`     | `false`  | vertical |
| listType   | Choose the icon that separates each link                                      | `'none' / 'circle' / 'square' ` | `false`  | `circle` |

<!-- @vuese:UnorderedList:props:end -->

## Slots

<!-- @vuese:UnorderedList:slots:start -->

| Name    | Description  | Default Slot Content |
| ------- | ------------ | -------------------- |
| default | list content | -                    |

<!-- @vuese:UnorderedList:slots:end -->
