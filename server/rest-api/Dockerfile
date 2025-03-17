# Use the official Node.js image as the base image
FROM node:14

# Set the working directory
WORKDIR /app

# Copy package.json and package-lock.json
COPY package.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application code
COPY . .

# Expose the port the app runs on
EXPOSE 8000

ENV CHOKIDAR_USEPOLLING=true

# Command to run the application
CMD ["sh", "-c", "if [\"$NODE_ENV\" = \"production\"]; then npm run start; else npm run dev; fi"]