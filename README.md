# Dispatch

Dispatch is an API Gateway written in Java which aims to be simple, lightweight, and extensible.

Dispatch uses Jetty and Virtual Threads.

## Starting the server

```
mvn compile exec:java -Dexec.mainClass="com.dispatch.server.Main"
``` 

## Benchmarking

```
echo "GET http://localhost:8080/" | vegeta attack -duration=30s -rate=1000 -output=results.bin && cat results.bin | vegeta plot --title="Load Test Results" > load-test
```

 