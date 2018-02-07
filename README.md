# Welcome

This project contains a sample Spring boot based service that exposes an endpoint of <code>/hello</code>.

The service connects to a Postgres database to retrieve the welcome message to return.

# Build & Test

To build and test the service execute the following command:

    mvn clean package

# Run

To run the service you must first have postgres running.  You could do this via Docker:

    docker run -p 5432:5432 --name postgres postgres:9.4

Then run the service itself by executing the following command:

    mvn spring-boot:run -Drun.jvmArguments="-Dspring.datasource.username=postgres"