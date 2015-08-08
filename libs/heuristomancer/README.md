# heuristomancer

A Clojure library for attempting to guess file types.

Heuristomancer uses EBNF files defining context-free grammars with [instaparse](https://github.com/engelberg/instaparse) and a provided sample size (default 1000 bytes). EBNF grammars can be found in `resources/ebnf/` and are indexed by `resources/grammars.clj` for use by Heuristomancer.

## TL;DR

### Command-Line Usage

```bash
# Display the help text.
java -jar /path/to/heuristomancer-0.1.0-SNAPSHOT-standalone.jar -h

# List the file types recognized by heuristomancer.
java -jar /path/to/heuristomancer-0.1.0-SNAPSHOT-standalone.jar -l

# Identify the types of some files.
java -jar /path/to/heuristomancer-0.1.0-SNAPSHOT-standalone.jar file1 file2

# Use a different sample size when identifying files.
java -jar /path/to/heuristomancer-0.1.0-SNAPSHOT-standalone.jar -s 2000 file
```

### Programmatic Usage

```clojure
(use 'heuristomancer.core)

;; Identify a file using the default sample size (1000 characters).
(identify "/path/to/file")

;; Identify a file with a specified sample size (in number of characters).
(identify "/path/to/file" sample-size)

;; Identify a sample that's already loaded into memory.
(identify-sample sample)
```

## License

http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt
