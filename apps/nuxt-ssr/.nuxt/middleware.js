const middleware = {}

middleware['emx2'] = require('../middleware/emx2.js')
middleware['emx2'] = middleware['emx2'].default || middleware['emx2']

export default middleware
