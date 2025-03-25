# Networked Producer and Consumer Application

This project implements a networked system for video uploading and consumption. It consists of two main components: a producer application that uploads video files to a consumer application, and a consumer application that receives, stores, and serves those videos.

## Project Overview

* **Producer Application:** A Java application that reads video files from a specified directory and uploads them to the consumer application via a socket connection.
* **Consumer Application:** A Spring Boot application that listens for video uploads on a socket, stores them, and provides a web interface for viewing and playing the uploaded videos.

## Prerequisites

* **Java 17 or later:** Ensure you have Java 17 or a later version installed.
* **Maven or Gradle:** You'll need either Maven or Gradle to build the project.
* **Docker (Optional):** If you want to build and run the consumer application in a Docker container, you'll need Docker installed.

## Project Structure

* **Producer Application:**
    * `producer/`: Contains the producer application.
* **Consumer Application:**
    * `consumer_app/`: Contains the Spring Boot application code.

## Building and Running the Producer Application

1.  **Clone the Repository:**

    ```bash
    git clone https://github.com/Andre0819/stdiscm-problemset3.git
    cd producer
    ```

2.  **Create a `producer_videos` directory:**

    ```bash
    mkdir producer_videos
    ```

3.  **Place video files:** Place your video files (.mp4, .avi, .mov) into the `producer_videos` directory.
4.  **Compile and Run the Producer:**

    ```bash
    javac Producer.java
    java Producer
    ```

    * Alternatively, you can use an IDE to run the `Producer.java` main method.
    * You may need to change the consumer host, and port in the Producer.java file.

## Building and Running the Consumer Application

1.  **Clone the Repository:**

    ```bash
    git clone https://github.com/Andre0819/stdiscm-problemset3.git
    cd consumer_app
    ```

2.  **Build the Spring Boot Application:**

    * **Maven:**

        ```bash
        mvn clean install
        ```

    * **Gradle:**

        ```bash
        gradle clean build
        ```

    This will create a JAR file in the `target` directory (Maven) or `build/libs` directory (Gradle).

3.  **Run the JAR File:**

    ```bash
    java -jar target/*.jar # For Maven
    # or
    java -jar build/libs/*.jar # For Gradle
    ```

    The application will start, and you can access the web interface at `http://localhost:8080`.

## Building and Running the Consumer Application with Docker (Recommended)

1.  **Build the Docker Image:**

    ```bash
    docker build -t consumer-app .
    ```

    This command builds a Docker image named `consumer-app` using the Dockerfile in the `consumer_app` directory.

2.  **Run the Docker Container:**

    ```bash
    docker run -p 8080:8080 -p 12345:12345 consumer-app:latest
    ```

    This command runs the Docker container and maps port 8080 and 12345 on your host machine to the corresponding ports in the container.

3.  **Access the Application:**

    You can now access the web interface at `http://localhost:8080`.

## Dockerfile Explanation

The Dockerfile in the `consumer_app` directory contains the following instructions:

* `FROM openjdk:17-jdk-slim`: Uses a lightweight OpenJDK 17 image as the base.
* `WORKDIR /app`: Sets the working directory inside the container.
* `COPY target/*.jar app.jar`: Copies the Spring Boot JAR file into the container.
* `EXPOSE 8080`: Exposes port 8080 for the Spring Boot application.
* `EXPOSE 12345`: Exposes port 12345 for the socket server.
* `CMD ["java", "-jar", "app.jar"]`: Runs the Spring Boot application.

## Important Notes

* **Video Uploads:** The producer application uploads videos to the consumer application's socket server on port 12345.
* **Video Storage:** The consumer application stores uploaded videos in the `uploaded_videos` directory.
* **Web Interface:** The consumer application's web interface displays the uploaded videos and allows users to preview and play them.
* **Configuration:** You can customize the consumer application's configuration by modifying the `application.properties` file.
