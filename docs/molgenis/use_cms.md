# Using the MOLGENIS CMS

> NOTE
> The MOLGENIS CMS is currently in development. In this guide, we've provided a brief overview of the CMS, but things may change or may not work as described. If you notice any issues, please let us know.

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

In the page builder, all changes are saved automatically, whereas you must routinely save changes when using the page editor.

## The Page Builder

In the page builder, pages are created using a system of _blocks_ and _components_. Components&mdash;paragraphs, headings, images, visualisations, etc.&mdash;are placed inside blocks (or sections), which are placed within a page. By grouping content this way, this allows flexibility in how content is organised on a page. By default, new pages are created with a few basic elements: one page header and the first section. You may use these as a starting point or delete them from your page.

### How to create a page

1. Add a Header (Layouts >> Header): Set the page header, subtitle, and background image
2. Add a block (Layouts >> Section)
3. Add a heading (Components >> Heading) into the new section
4. Add a paragraph, image, or other component
5. Repeat for additional sections

#### Tips

- It is recommended that pages have no more than one header block
- Sections can be divided into columns allowing content to be placed side-by-side
- To add visual distinction between sections, use the "apply shaded background" option
- When adding headings to sections, do not use the `level` option to style the heading. This is used to define the page hierarchy. The level of the primary section heading should be 2 and all following headings should start at level 3.

### Adding visualisations

1. __Create a column chart component__: 
    - Drag the visualisation component into a section
    - Configure the component by defining the chart type, name, etc.
2. __Add data__:
    - By default, no data is added to the chart. This must be done manually. It is recommended to go to the Statistical chart data and download the table to use as a template. (You can also go to the Statistical Chart Data table and add new rows manually, but this may be tedious if you have many data points.)
    - In the file, remove all unrelated rows
    - For column charts, you will need to provide values for these columns: `id`, `x value`, `y value`
    - If you want to group data, enter values in `primary group value`
    - You will also need to copy the chart id (found in the form) and paste it in the `displayed in chart` column. Repeat for all data points you wish to display in the chart.
    - It is also recommended to define the `sort order`. By default, the visualisation components do not sort components. This only happens when the data is queried. If no order is defined, data will be rendered according to how it was imported into the table.
    - Import the file in to the CMS schema and refresh your page to view the chart
3. __Configure the chart__: once data is in available, you can now adjust the axes, add color palettes, etc. 

#### Tips

- All charts should have a title. A description is also useful for adding additional context for the user
- It is recommend to give a title for continuous axes
- Do not display dozens of values in a chart. Instead, use a table or group smaller values into an "other" category
- Do not create a color palette for styles each data point. This does not add value or meaning to the visualisation, and it is confusing for the reader. Color palettes should be used is when charts are grouped.
- If hover events are enabled, some labels may extend outside of the chart margins. This can be fixed by increasing the top margin.

## Troubleshooting

### How can I select my own photo to display in the page header?

For now, images can be imported in to the Images table (cms >> Images). Go to the table and click the "add images" button, and then select an image to import.

#### Tips

- We recommend compressing your image before importing into the CMS. This ensures that pages have fast load times.
- Give the image a `display name`. This makes it easier to select an image in the forms

### How do I get a section to display content in columns and rows?

It may be the case that you want to create a section that has multiple column layouts. For example, you may have a section with a heading and some text, but you want to display two images side-by-side. Changing the number of columns allows you to do this, but it will also arrange the heading and paragraphs. To get around this, follow these instructions.

1. __Create a section__: place all content you wish to display normally (top to bottom of the page)
2. __Create a section with columns__: set the number of columns you want to have and add content to the columns
3. __Remove section padding__: Click the edit section with columns button and scroll to "enable top padding" and/or "enable bottom padding" fields. This gives the appearance that the content is part of the same section.

### How do I create a page with my own theme?

You will need to create a new MOLGENIS-tailwind theme and add it to the GitHub repository. It would be best to get in touch with us at [support@molgenis.org](mailto:support@molgenis.org).

