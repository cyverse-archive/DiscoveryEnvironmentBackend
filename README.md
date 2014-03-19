# lein-deploy-fake-jars

A Leiningen plugin to deploy fake source and javadoc JAR files to a repository.
This is useful for Sonatype, which requires these JAR files to be deployed
before a project can be synchronized with the central repository.

## Usage

Put `[lein-deploy-fake-jars "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your project.clj.

```
$ lein deploy-fake-jars
```

## License

http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt
