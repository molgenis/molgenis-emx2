# AppPage

The `<Page/>` component is the primary layout component for creating pages within a vue application. Pages should then be built using other page layout components (`<PageHeader>`, `<PageSection>`, `<PageFooter>`, etc.). For example, if we were to outline a typical structure of a page, it would look like this. If you have more than one page, it is recommended to create a custom footer, and import it at the `App.vue` level. See the PageFooter guide for more details.

## Slots

<!-- @vuese:AppPage:slots:start -->

| Name    | Description      | Default Slot Content |
| ------- | ---------------- | -------------------- |
| default | Main app content | -                    |

<!-- @vuese:AppPage:slots:end -->
