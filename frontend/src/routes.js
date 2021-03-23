import catalogueRoutes from './routes/catalogue.js'
import pagesRoutes from './routes/pages.js'
import schemaRoutes from './routes/schema.js'
import settingsRoutes from './routes/settings.js'
import tableRoutes from './routes/table.js'

export default [
  ...catalogueRoutes,
  ...pagesRoutes,
  ...schemaRoutes,
  ...tableRoutes,
  ...settingsRoutes,
]
