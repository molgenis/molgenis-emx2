import catalogueRoutes from './routes/catalogue.js'
import pagesRoutes from './routes/pages.js'
import schemaRoutes from './routes/schema.js'
import settingsRoutes from './routes/settings.js'
import tablesRoutes from './routes/tables.js'

export default [
  ...catalogueRoutes,
  ...pagesRoutes,
  ...schemaRoutes,
  ...tablesRoutes,
  ...settingsRoutes,
]
