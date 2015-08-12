# clockwork

Scheduled jobs for the iPlant Discovery Environment.

## Build

Periodically -- as in once a day or so -- run the following command from the top-level of the backend checkout:

    > docker run --rm -it -v ~/.m2/:/root/.m2/ -v $(pwd):/build -w /build discoenv/buildenv lein exec build-all.clj lein-plugins libs

That will build the latest version of all of the libraries for the backend and place them into your local .m2 directory. As annoying as that is to type, it's still less annoying than trying to get a full development environment set up on your local box.

To build a new version of anon-files run the following inside the services/anon-files/ directory of the checkout (which contains this file):

    > docker run --rm -v ~/.m2/:/root/.m2/ -v $(pwd):/build -w /build discoenv/buildenv lein uberjar
    > docker build -t discoenv/clockwork:dev .

The build of the uberjar is separate from the build of the container image to keep the size of the container image a bit more reasonable.

## Usage

To update and run clockwork locally, run the following two commands:

```
$ docker pull discoenv/clockwork:dev
$ docker run -P -d --name clockwork -v /path/to/config:/etc/iplant/de/clockwork.properties discoenv/clockwork:dev
```

You can skip the first command if you've built the clockwork Docker container image locally.

Clockwork gets its configuration settings from a configuration file. The path
to the configuration file is given with the --config command-line setting.

## License

Copyright (c) 2011, The Arizona Board of Regents on behalf of
The University of Arizona

All rights reserved.

Developed by: iPlant Collaborative as a collaboration between participants at BIO5 at The University
of Arizona (the primary hosting institution), Cold Spring Harbor Laboratory, The University of Texas
at Austin, and individual contributors. Find out more at http://www.iplantcollaborative.org/.

Redistribution and use in source and binary forms, with or without modification, are permitted
provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions
   and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
   and the following disclaimer in the documentation and/or other materials provided with the
   distribution.

 * Neither the name of the iPlant Collaborative, BIO5, The University of Arizona, Cold Spring Harbor
   Laboratory, The University of Texas at Austin, nor the names of other contributors may be used to
   endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
