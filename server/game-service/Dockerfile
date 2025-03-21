# Use the official Golang image as the base image
FROM golang:1.23 AS builder

# Set the working directory
WORKDIR /app

# Copy go.mod and go.sum files
COPY go.mod go.sum ./

# Download all dependencies. Dependencies will be cached if the go.mod and go.sum files are not changed
RUN go mod download

# Copy the source code into the container
COPY . .

# Build the Go application for the correct architecture
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -a -installsuffix cgo -o game-service .

# Use a minimal base image for the final container
FROM scratch

# Set the working directory
WORKDIR /app

# Copy the built Go binary from the builder stage
COPY --from=builder /app/game-service .

# Expose the port the app runs on
EXPOSE 8082

# Command to run the application
CMD ["./game-service"]