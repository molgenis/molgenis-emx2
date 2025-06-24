# Analytics module

Goal of the module is to allow service maintainers to track certain user actions. 
For example maintainer of a Data Catalogue application might what to know when and by who a 'Contact' button is clicked.

This module when installed sets up 'Triggers' attched to page elements ( for example a Button in a given location).
Besides the need to install and configure this module in a app ( or compoment ),
the triggers ( what events to run when ) should be created using the analytics api or web ui.

### Install the module 

use package manager to install the module '@molgenis/emx2-analytics'

```yarn add @molgenis/emx2-analytics``` (or use the * option for yarn workspaces)

### Setup the triggers in the app 
```import { setupAnalytics } from "@molgenis/emx2-analytics"; ```

...

```  setupAnalytics(schema, providers);```

schema: The name of the emx schema/database

providers: A list of analytics profiders and there config options, for example; ```providers = [{ id: "site-improve", options: { analyticsKey } }];```

This setup should be run before the user interacts with the page

During the setupAnalytics call 3 steps are taken
 - 1. For each provider the nessasary code is (fetched and) loaded 
 - 2. The triggers configured for this schema are fetched from the backend
 - 3. For each triggers the DOM elements are located in the page and a eventhandler gets attached for the configured provider

 When the end user visits the page and triggers the event the attached eventhandler uses the provider script to send the event. 

 The whole analyics module works an a fire-and-forget basis, if something goes wrong the end user is not notified ( except for the browser console error log)


## Development

includes playground 'app', run via `yarn dev`

## Build

`yarn build`

## Release 

todo