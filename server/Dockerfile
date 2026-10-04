FROM node:22-alpine
WORKDIR /app
COPY package.json server.js server.test.js ./
RUN npm install --omit=dev
EXPOSE 8080
CMD ["npm","start"]