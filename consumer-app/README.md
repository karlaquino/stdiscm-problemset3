# Consumer Application

This application receives, processes, and serves video content uploaded by the Producer application.

## Setup and Deployment

The simplest way to set up the Consumer application is using the provided Docker script:

```bash
./dockerize.sh
```

This script builds a Docker image and runs the container with all necessary port mappings and configurations.

## Manual Deployment

If you prefer to run without Docker:

1. Build the application:
   ```bash
   ./gradlew build
   ```

2. Run the application:
   ```bash
   java -jar build/libs/consumer-app.jar
   ```

## Accessing the Application

Once running, you can access the web interface at:
- http://localhost:8080

## Configuration

The application is configured to:
- Listen for video uploads on port 12345
- Store uploaded videos in the `uploaded_videos` directory
- Process videos according to configured settings

You can customize these settings in the `application.properties` file. 