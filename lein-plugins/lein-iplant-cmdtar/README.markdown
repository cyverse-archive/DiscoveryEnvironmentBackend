# Leiningen Plugin for Generating Binary Command Tarballs

This is a Leiningen plugin that can be used to generate binary tarballs for
command-line utilities written in clojure.  The generated tarball will contain
an uberjar along with a Bash script that is automatically generated to invoke
the program.

## Leiningen 1 Compatibility

This plugin is not compatible with Leiningen 1 at this time.

## Usage

This plugin is stored in iPlant's Archiva repository, which will have to be
defined in `project.clj`:

```clojure
:repositories {"iplantCollaborative"
               "http://projects.iplantcollaborative.org/archiva/repository/internal/"}
```

Next, the plugin has to be added to the `:plugins` section of `project.clj`:

```clojure
[org.iplantc/lein-iplant-cmdtar "0.1.0-SNAPSHOT"]
```

No other configuration is necessary; the generated Bash script will
automatically be named after the project itself.  To generate the tarball,
execute the following command:

```
lein iplant-cmdtar
```

It's not necessary to compile the project first because the `uberjar` task
will be called automatically.

## License

Copyright (c) 2012, The Arizona Board of Regents on behalf of The University
of Arizona

All rights reserved.

Developed by: iPlant Collaborative as a collaboration between participants at
BIO5 at The University of Arizona (the primary hosting institution), Cold
Spring Harbor Laboratory, The University of Texas at Austin, and individual
contributors. Find out more at http://www.iplantcollaborative.org/.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
 * Neither the name of the iPlant Collaborative, BIO5, The University of
   Arizona, Cold Spring Harbor Laboratory, The University of Texas at Austin,
   nor the names of other contributors may be used to endorse or promote
   products derived from this software without specific prior written
   permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
