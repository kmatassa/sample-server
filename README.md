# http-server 

This is a sample thread-pooled http server, very simplified to only service GET requests to static files found in this repository.  

It also has a Keep-Alive extension that implements this functionality with some limitations.  It specifically is enabled for:

- http 1.1
- max request optional
- timeout on idle optional 

It works on Windows and Mac.

## Prerequisites

- http://www.oracle.com/technetwork/java/index.html
- https://maven.apache.org

## Run it.

Assuming you have a maven client installed, it should be very simple:

1. mvn test<CR>

This will run the sequence of maven targets up to and including test.  Items should be installed, code check-styled, compiled and finally
test cases that start the http-server up and execute simplified fetches against it.

### Inspect test results

- shouldAnswerWithDisplayedContent - Displays an html result
- shouldAnswerWithPDFContentInSubDirectory - Downloads a PDF from a nested subdirectory
- shouldAnswerWithPNGContent - Downloads a PNG
- shouldFailWithNotFound - Returns an expected error for a non-existing file
- shouldFailWithMethodNotAllowed - Returns an expected error for invalid operation
- shouldAnswerWithHEADinfo - Returns head only information

- keepAliveWithHttp1_1_is_supported - illustrates keep-alive keeps socket open
- keepAliveWithHttp1_1_is_supported_but_limited_to_2_max_by_client - illustrates keep-alive max parameter
- keepAliveWithHttp1_1_is_supported_but_disabled_by_client - illustrates client override to disable
- keepAliveWithHttp1_0_is_not_supported - illustrates 1.0 is not a supported version for keep-alive

OR,

mvn mvn exec:java<CR>

This will do the same, winding up with the http-server staying up so that you can make browser requests to it locally, such as:

Then you can hit: "http:localhost:8081/helloworld.html" (includes a nested image file)

Then you can hit: "http:localhost:8081/ERROR_chrome_2018-08-01T20-43-00.756Z.png"

Then you can hit: "http:localhost:8081/foo/introducing_cairngorm.pdf"

ctrl<C> to terminate.

## Methodology

I've created a sample http-server.  My approach was as follows:

- start
  - spin 1 thread to start processing loop with socket wait
    - for each request
      - use a fixed size thread pool to service each request
        - within a request, service either just 1 or multiple HTTP requests based on keep-alive

This seems to work both either in standalone mode where one manually hits the server as described above, or as part of the test suite where
the suite initializes first with spinning up the server and then subsequently running tests.

## Shortcuts

I utilized the following libraries:

- https://commons.apache.org/proper/commons-io/ - For file and string manipulation.
- https://hc.apache.org/httpcomponents-client-ga/index.html - For httpClient
- http://hc.apache.org/httpcomponents-core-ga/ - For HttpRequestLine and headers
- https://junit.org/junit5/docs/current/api/overview-summary.html - Junit support


