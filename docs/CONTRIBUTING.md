# Contributing

## Building this Project

Assemble a portable JAR with the provided bootstrap script. Easy!

```text
❯ ./mill tools.localJar
❯ ls -1 jars
cvbio.jar
```

## Using this Project

```text
❯ java -jar jars/cvbio.jar -h
USAGE: cvbio [cvbio arguments] [command name] [command arguments]
Version: 1.1.0
...
```
