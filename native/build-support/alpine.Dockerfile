
FROM amazoncorretto:17.0.12-alpine3.18

# Install required dependencies
RUN apk add --no-cache bash alpine-sdk cmake openssl-dev openssl

# Create a non-root user
RUN adduser -D user
USER user