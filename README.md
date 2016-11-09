Undertaking
===========

[![Build Status](https://travis-ci.org/zalando-incubator/undertaking.svg?branch=master)](https://travis-ci.org/zalando-incubator/undertaking)
[![codecov.io](http://codecov.io/github/zalando-incubator/undertaking/coverage.svg?branch=master)](http://codecov.io/github/zalando-incubator/undertaking?branch=master)

A toolbox for Undertow/Rx/Guice.


## Release process
The shell script `release.sh` can be used to prepare a release. It performs
the following steps:

  * create a new branch for the release commits
  * create a commit and a tag for the new release version
  * verify that tag by invoking `./mvnw clean verify`
  * increment to the next development version and commit it
  * push the new branch to the remote repository
  * TODO: automatically open a Pull Request

After the pull request has been merged, the tag can be pushed:

    git push origin <TAG_NAME>
    
## Specific instructions
###  Usage of the LogbookHandler
The LogbookHandler expects an obfuscation function of type `Function<String, String>` 
to be injected. This function prevents confidential path parameters in the URI to be 
logged as plain text.


## License

The MIT License (MIT)

Copyright © 2016 Zalando SE, https://tech.zalando.com

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
