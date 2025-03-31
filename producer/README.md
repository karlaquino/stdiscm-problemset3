## Producer App

Thread pool size is defined via environment variable `PRODUCER_THREAD_POOL_SIZE`

JAR File is located in the `out` directory

To run:
- Build docker image
- `docker build -t producer-image .`
- Run container
- `docker run -p 3000:3000 -e PRODUCER_THREAD_POOL_SIZE=n producer-image:latest`