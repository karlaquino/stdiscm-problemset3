# Producer Application

This Java application processes video files and uploads them to the consumer service.

## Initial Setup

Before running the Producer for the first time, you must initialize the environment:

```bash
./initialize.sh
```

This script prepares the necessary directories and sets up required dependencies.

## Running the Producer

### Method 1: Standard Execution

```bash
javac -d ./out src/main/java/com/garynation/Producer.java
java -cp ./out com.garynation.Producer
```

### Method 2: With Custom Thread Pool Size

You can configure the Producer's thread pool size by setting an environment variable:

```bash
export PRODUCER_THREAD_POOL_SIZE=10
javac -d ./out src/main/java/com/garynation/Producer.java
java -cp ./out com.garynation.Producer
```

## Video Processing

Place your video files in the `producer_videos` directory. The Producer will automatically process and upload these files to the consumer service.

## Configuration

You can modify the Producer's behavior by editing the following parameters in the source code:

To run:
- Build docker image
- `docker build -t producer-image .`
- Run container
- `docker run -p 3000:3000 -e PRODUCER_THREAD_POOL_SIZE=n producer-image:latest`