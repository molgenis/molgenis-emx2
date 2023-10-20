# Accordion

Create a collapsible element for hiding and showing content. For example, the accordion component is a good option for structuring an FAQ page. Accordion state (i.e., open or closed) can be accessed using the following event `@isOpen`.

## Props

<!-- @vuese:Accordion:props:start -->

| Name            | Description                                     | Type      | Required | Default |
| --------------- | ----------------------------------------------- | --------- | -------- | ------- |
| id              | A unique identifier for the accordion           | `String`  | `true`   | -       |
| title           | A label that describes the hidden content       | `String`  | `true`   | -       |
| isOpenByDefault | If true, the accordion will be opened on render | `Boolean` | `false`  | false   |

<!-- @vuese:Accordion:props:end -->

## Events

<!-- @vuese:Accordion:events:start -->

| Event Name | Description | Parameters |
| ---------- | ----------- | ---------- |
| isOpen     | -           | -          |

<!-- @vuese:Accordion:events:end -->

## Slots

<!-- @vuese:Accordion:slots:start -->

| Name    | Description                   | Default Slot Content |
| ------- | ----------------------------- | -------------------- |
| default | Content to be hidden or shown | -                    |

<!-- @vuese:Accordion:slots:end -->
