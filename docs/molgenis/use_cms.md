# Using the MOLGENIS CMS

The MOLGENIS CMS&mdash;or _content management system_&mdash;allows users to create custom interfaces within a molgenis instance. Behind the scenes, the CMS is a central schema, which is called `cms` and comes pre-loaded with a few example pages that you can use as a starting point for creating your own pages.

In the CMS, there are two types of pages that can be created, which can only be created if a user has manager-level permissions.

1. __Landing page__: a simple, drag-and-drop interface where users can create content from a set of standard MOLGENIS components (e.g., paragraphs, lists, images, visualisations, etc.)
2. __Developer page__ (advanced): create a page from the scratch using HTML, CSS, and JavaScript.

All CMS pages, by default, are connected to the local MOLGENIS theme ("light" and "dark"), so you do not need to worry about styling pages.

## How to use the MOLGENIS CMS

MOLGENIS CMS pages are always visible in the __page gallery__. To edit a page-

1. Go to your molgenis instance and sign in (must have manager or a higher permission level)
2. Go to the page gallery at `https://<host>/apps/ui/cms/pages`
3. Find the page you wish to edit and click the corresponding pencil icon.
    - If are editing a landing page, you will be redirected to the __page builder__, which is the drag-and-drop interface
    - For developer pages, the __page editor__ will open and an IDE-like interface will be displayed
4. Start building your page

In the page builder, all changes are saved automatically, whereas you must routinely save changes in the page editor.

## The Page Builder

In the page builder, pages are created using a system of _blocks_ and _components_. _Blocks_, such as a page section or page header, are used to group _components_ with in a page. This system allows you to organise content and manage groups of content much easier.

1. Add a Header: Set the page header, subtitle, and background image
2. Add a section
3. Add a heading to the section
4. Add additional content (e.g., paragraphs, images, files, etc.)

#### Tips

- Pages should have no more than one header block
- Sections can be divided into columns allowing content to be placed side-by-side
- To add visual distinction between sections, use the "apply shaded background" option
- When adding headings to sections, do not use the `level` option to style the heading. This is used to define the page hierarchy. The level of the primary section heading should be 2 and all following headings should start at level 3.

### Adding visualisations

1. __Create a column chart component__: 
    - Drag the visualisation component into a section
    - Configure the component by defining the chart type, name, etc.
2. __Add data__:
    - Data must be added manually. It is recommended to go to the Statistical chart data and download the table to use as a template. Open the file and remove all rows to have a blank template. You can also go to the Statistical Chart Data table and add new rows manually, but this may be tedious if you have many data points.
    - For column charts, you will need to provide values for these columns: `id`, `x value`, `y value`
    - If you want to group data, enter values in `primary group value`
    - You will also need to copy the chart id (found at the end of the form) and paste it in the `displayed in chart` column. Repeat for all data points you wish to display in the chart.
    - It is also recommended to define the `sort order`. By default, the visualisation components do not sort components. This only happens when the data is queried. If no order is defined, data will be rendered according to how it was originally imported.
    - Import the file in to the CMS schema and refresh your page to view the chart
3. __Configure the chart__: once data is in available, you can now adjust the axes, add color palettes, etc. 

#### Tips

- All charts should have a title. A description is also useful for adding additional context for the user
- It is recommend to give a title for continuous axes
- Do not display dozens of values in a chart. Instead, use a table or group smaller values into an "other" category
- Do not create a color palette for styles each data point. This does not add value to the visualisation and it is confusing for the reader. Only use color palettes when charts are grouped.
- When hovering over chart elements, labels may extend beyond the dimensions of the chart. This can be fixed by increasing the margins to allow for more space for the labels. Alternatively, try revising the value or label for the point(s) that overflow. 

## Troubleshooting

### How can I select my own photo to display in the page header?

Images are stored in the CMS/Images table. New files can be directly imported into this table.

1. Go to the images table: `https://<host>/apps/ui/cms/Images`
2. Click the "Add images" button
3. Select the image to import. Give the image a `display name` to makes it easier to select this image in the page builder
4. Save
5. Go page to page builder (or refresh). Click the "edit page header" button. The image should now be listed in the `background image` field.

#### Tips

- We recommend compressing your image before importing into the CMS. This ensures that pages have fast load times.

### How do I get a section to display content with and without columns?

There are some situations you would like to display content as columns, such as two or more images side-by-side, and have other content outside of the column layout. The section component has an option to set the number of columns, which will split a section into columns.

1. __Create a section__: place all content you wish to display normally (top to bottom of the page)
2. __Create a section with columns__: set the number of columns you want to have and add content
3. __Remove section padding__: Click the edit section with columns button and scroll to "enable top padding" and/or "enable bottom padding" fields. This gives the appearance that the content is part of the same section.

### How do I create a page with my own theme?

It isn't not possible to do this using the page builder. You will need to create a new MOLGENIS-tailwind theme and add it to the MOLGENIS EMX2 software. It would be best to get in touch with us at [support@molgenis.org](mailto:support@molgenis.org).


### How do I add a filter to the data highlights component?

In the data highlights component, there's an option to enter your own value or retrieve a value from the graphql API. Using the API, you can retrieve the counts, sum, mean, min, and max. Filters are also supported, but these can be tricky to write.

In the query, make sure-

- properties are quoted
- `_and` statements are separated by a comma
- Brackets are closed

If there is an issue with the query, the `N/A` value will be shown.

Here are some examples of queries:

```graphql
// number of cats
{"category":{"name": {"equals":["cat"]}}}

// number of cats that are available
{"_and": [{"category":{"name": {"equals":["cat"]}}}, {"status": {"equals": ["available"]}}]}
```