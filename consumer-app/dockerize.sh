# build the consumer-app .jar
./gradlew build

# build consumer-app image
docker build -t consumer-app-image .

# run container
docker run -p 8080:8080 -p 12345:12345 consumer-app-image
