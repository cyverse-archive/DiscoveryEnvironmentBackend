# kifshare

A simple web page that allows users to download publicly available files without an iPlant account.

# Development Prerequisites

* iRODS 3.1 or later - http://irods.org
* Leiningen 2 - http://leiningen.org
* Node.js - http://nodejs.org/
* Grunt - http://gruntjs.com/
* LESS - http://lesscss.org/

On OS X, you'll also want homebrew installed to help with getting Node.js and Grunt up and running. It'll help you stay sane. Once homebrew is installed, do the following on the command-line:

    brew install node.js
    <add npm to your $PATH as instructed>

You'll need an iRODS install that's of version 3.1 or later. It doesn't need to live on the same box as kifshare, but kifshare will need to be able to connect to it.

You'll need Leiningen 2 installed as well. See http://leiningen.org for more details.

Once all of the above is downloaded, installed, and configured, go into the projects top directory (the one containing package.json) and run the following:

    npm install

That should download and install the development dependencies. If you have trouble running grunt or lesscss after this, try the following:

    npm install -g grunt-cli
    npm install -g less

Again, only do that if the bare 'npm install' doesn't work.

# Upgrading to Grunt 0.4.X

We're now using Grunt 0.4.X for Kifshare, so you'll want to upgrade if you can. Upgrade instructions are available here:

    http://gruntjs.com/getting-started#grunt-0.3-notes

# Configuring Kifshare For Development

Create a .properties file using docs/sample.properties as a template.

# Configuring mod_proxy in Apache for kifshare

All downloads flow through the /d/ endpoint in kifshare. So, to proxy download requests to another box, try the following in one of the httpd config files.

    ProxyPass /quickshare/d http://kifshare-downloads.example.org:31380/d retry=0
    ProxyPassReverse /quickshare/d http://kifshare-downloads.example.org:31380/d

Next, you need to set up ProxyPass entries for the UI:

    ProxyPass /quickshare http://kifshare.example.org:31380 retry=0
    ProxyPassReverse /quickshare http://kifshare.example.org:31380

The entries must be in that order. The lack of trailing slashes is important.

Another potential issue is the "DefaultType" setting in /etc/httpd/conf/httpd.conf. If it's set to text/plain, then kifshare's UI will show up as plain text. If it's set to None, then kifshare's UI works as expected. So, set the DefaultType to None in /etc/httpd/conf/httpd.conf, like in the following:

    DefaultType None

# Building Kifshare For Development

You'll build kifshare with Grunt. Grunt, in turn, calls Leiningen when necessary.

If you're only working on the Clojure portion of the code, then you only need to call this to build the resources directory:

    grunt build-resources

Then you can use Leiningen as you normally would.

If you want to do a build of both the front-end and the clojure code:

    grunt build-all

If you want to clean everything (including resources):

    grunt clean-all

If you only want to clean the resources directory:

    grunt shell:clean_resources

The following commands are also included:

    grunt shell:lein_clean
    grunt shell:lein_deps
    grunt shell:lein_uberjar

# Running Kifshare

If you're working out of a git checkout, then you can run the following once the resources directory is built (see the preceding section):

    lein run --config </path/to/config/file>

If you're running from a jar, then you should be able to run the following:

    java -jar <kifshare-jar> --config </path/to/config>

If you're running it from the RPM with Zookeeper, then run the following:

    sudo /sbin/service kifshare start

# Downloading through a browser

Hit kifshare with a browser, setting the path portion of the URL to the name of the publicly available iRODS ticket.

    http://<kifshare-host>:<kifshare-port>/<ticket-name>

Once the kifshare page comes up, click on the big "Download" button.

# Downloading with curl

This requires you to know the name of the file referred to by the ticket.

    curl http://<kifshare-host>:<kifshare-port>/d/<ticket-name>/<filename>
