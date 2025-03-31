# build the consumer-app .jar
./gradlew build

# build consumer-app image
docker build -t consumer-app-image .

# run container
docker run -p 8080:8080 -e VIDEO_UPLOAD_THREAD_POOL_SIZE=4 -e VIDEO_UPLOAD_QUEUE_SIZE=2 consumer-app-image
