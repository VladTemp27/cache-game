# Use the official Node.js image as the base image
FROM node:18

# Set the working directory
WORKDIR /app

# Copy package.json and package-lock.json
COPY package.json package-lock.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application code
COPY . .

# Expose the port the app runs on
EXPOSE 8080

ENV CHOKIDAR_USEPOLLING=true

# Command to run the application
CMD ["sh", "-c", "if [  \"$NODE_ENV\" = \"production\"  ]; then echo \"Running in production mode\" && npm start; else echo \"Running in development mode\" && npm run dev; fi"]