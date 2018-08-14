# http-server 

This is a sample thread-pooled http server, simplified to only service Get & Head requests to static files found in this repository.  Requests given to threads 
that are managed via a thread pool fixed in size to 10 threads.

It also has the optional implementation of a  Keep-Alive extension that implements this functionality with some limitations.  It specifically is enabled for:

- http 1.1
- max request optional
- timeout on idle optional 

http-server works on Windows and Mac.

## Prerequisites

- http://www.oracle.com/technetwork/java/index.html
- https://maven.apache.org

## Run it

Assuming you have a maven client installed, it should be very simple:

```
mvn test
```

This will run the sequence of maven targets up to and including test.  Items should be installed, code check-styled, compiled and finally
test cases that start the http-server up and execute simplified fetches against it.

### Inspect test results

Junit test cases have been built to illustrate functionality and are being run via the "test" goal from the surefire plugin.

- shouldAnswerWithDisplayedContent - Displays an html result
- shouldAnswerWithPDFContentInSubDirectory - Downloads a PDF from a nested subdirectory
- shouldAnswerWithPNGContent - Downloads a PNG
- shouldFailWithNotFound - Returns an expected error for a non-existing file
- shouldFailWithMethodNotAllowed - Returns an expected error for invalid operation
- shouldFailWithBlockedSecurityCheck - Returns an expected error for paths that seek past server root
- shouldAnswerWithHEADinfo - Returns head only information
- keepAliveWithHttp1_1_is_supported - illustrates keep-alive keeps socket open
- keepAliveWithHttp1_1_is_supported_but_limited_to_2_max_by_client - illustrates keep-alive max parameter
- keepAliveWithHttp1_1_is_supported_but_disabled_by_client - illustrates client override to disable
- keepAliveWithHttp1_0_is_not_supported - illustrates 1.0 is not a supported version for keep-alive
- keepAliveWithHttp1_1_is_supported_but_limited_to_timeout - illustrates keep-alive timeout parameter

Or,

You can run the app standalone and use a browser to make basic file request.

```
mvn exec:java
```

This will invoke the App main which will then pause waiting for HTTP request to service.  You can then try:

- hit: "http:localhost:8081/helloworld.html" (includes a nested image file)
- hit: "http:localhost:8081/ERROR_chrome_2018-08-01T20-43-00.756Z.png"
- hit: "http:localhost:8081/foo/introducing_cairngorm.pdf"

Press ctrl-C to terminate.

## Methodology

I've created a sample http-server only.  My approach was to not use too many high level libraries, but use a few where
it makes sense where illustration is not as beneficial.

- start
  - spin 1 thread to start processing loop with socket wait
    - for each request
      - use a fixed size thread pool to service each request
        - within a request, service either just 1 or multiple HTTP requests based on keep-alive

This supports both cases where the application is being run in standalone mode where one manually hits the server as described above, 
or as part of the test suite where the test suite initializes by starting the server and follows this by executing the suite.

## Javadoc

If desired, it will be produced in the /target/site/apidocs folder.

```
mvn javadoc:javadoc
```

## Shortcuts

I utilized the following libraries:

- https://commons.apache.org/proper/commons-io/ - For file and string manipulation.
- https://hc.apache.org/httpcomponents-client-ga/index.html - For httpClient
- http://hc.apache.org/httpcomponents-core-ga/ - For HttpRequestLine and headers
- https://junit.org/junit5/docs/current/api/overview-summary.html - Junit support
- https://docs.oracle.com/javase/7/docs/api/javax/activation/package-summary.html - For mimetypes


