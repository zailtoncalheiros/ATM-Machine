FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/atm-machine-0.0.1-SNAPSHOT-standalone.jar /atm-machine/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/atm-machine/app.jar"]
