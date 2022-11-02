####
## Build file for server side rendered (ssr) catalogue application for use with molgenis EMX2 backend
###

## Base image to have a node runtime
FROM node:18.12.0-alpine

# Set host to localhost / the docker image
ENV NUXT_HOST=0.0.0.0

ENV API_PROXY_TARGET=http://host.docker.internal:8080/

## Copy the files need from the contaxt into to image
COPY ./nuxt3-ssr /app/build

WORKDIR /app/build

## Clean files that where that should not have been copied
RUN rm -rf .output
RUN rm -rf .nuxt
RUN rm -rf node_modules

## Generate both server and client in production mode
RUN npm cache clean --force
RUN npm install
RUN npm run build

RUN mv /app/build/.output /app/.output
RUN mv /app/build/.nuxt /app/.nuxt
RUN rm -rf /app/build/

WORKDIR /app

ENV NUXT_HOST=0.0.0.0
ENV NUXT_PORT=3000

EXPOSE 3000

## Start the server
CMD node .output/server/index.mjs

