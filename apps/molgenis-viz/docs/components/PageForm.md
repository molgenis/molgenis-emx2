# PageForm

The `<PageForm>` component is the parent layout component for creating forms within an page. Ideally, this should be child element of `<PageSection />`.

## Props

<!-- @vuese:PageForm:props:start -->

| Name        | Description                                                       | Type     | Required | Default |
| ----------- | ----------------------------------------------------------------- | -------- | -------- | ------- |
| title       | A title for the form (passed down to `<legend>`)                  | `String` | `true`   | -       |
| description | An optional description of the form to provide additional context | `String` | `false`  | -       |

<!-- @vuese:PageForm:props:end -->

## Slots

<!-- @vuese:PageForm:slots:start -->

| Name    | Description  | Default Slot Content |
| ------- | ------------ | -------------------- |
| default | form content | -                    |

<!-- @vuese:PageForm:slots:end -->
