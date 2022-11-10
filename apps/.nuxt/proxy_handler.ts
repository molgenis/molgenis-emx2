import { createProxyMiddleware } from 'nuxt-proxy/middleware'

export default createProxyMiddleware({"target":"http://localhost:8080/","pathFilter":["**/*/graphql"]})