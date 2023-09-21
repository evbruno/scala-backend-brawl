FROM findepi/graalvm:java17-all
# FROM ghcr.io/graalvm/native-image-community:17
RUN gu install native-image
WORKDIR /app
ADD target/scala-2.13/app-0.1-SNAPSHOT.jar app.jar
RUN native-image \
  --static \
  --verbose \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  --no-fallback -jar app.jar http-app
ENTRYPOINT ["/app/http-app"]