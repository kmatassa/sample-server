# http-server 

This is a sample http server, very simplified to only service GET requests to static files found in this repository.

## Run it.

Assuming you have a maven client installed, it should be very simple:

1. mvn test<CR>

This will run the sequence of maven targets up to and including test.  Items should be installed, code check-styled, compiled and finally
test cases that start the http-server up and execute simplified fetches against it.

OR,

mvn integration-test<CR>

This will do the same, winding up with the http-server staying up so that you can make browser requests to it locally, such as:

"http:localhost:8080/helloworld.html"

## Methodology

My approach...

## Shortcuts

Code I lifted...
