FROM openjdk:11-jre-slim

ENV VERTICLE_FILE event-brigde-1.0-SNAPSHOT-jar-with-dependencies.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles
RUN mkdir -p $VERTICLE_HOME
EXPOSE 9092

# Copy your fat jar to the container
COPY target/$VERTICLE_FILE $VERTICLE_HOME
COPY src/main/resources/* $VERTICLE_HOME/
# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE"]
