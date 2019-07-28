# cvbio

[![Build Status][travis-badge]][travis-link]
[![Code Coverage][codecov-badge]][codecov-link]
[![Language][scala-badge]][scala-link]
[![Code Style][scalafmt-badge]][scalafmt-link]
[![Releases][releases-badge]][releases-link]
[![License][license-badge]][license-link]

[codecov-badge]:  https://codecov.io/gh/clintval/cvbio/branch/master/graph/badge.svg
[codecov-link]:   https://codecov.io/gh/clintval/cvbio
[license-badge]:  https://img.shields.io/badge/license-MIT-blue.svg
[license-link]:   https://github.com/clintval/cvbio/blob/master/LICENSE
[releases-badge]: https://img.shields.io/badge/cvbio_Releases-555555.svg
[releases-link]:  https://github.com/clintval/cvbio/releases
[scala-badge]:    https://img.shields.io/badge/language-scala-c22d40.svg
[scala-link]:     https://www.scala-lang.org/
[scalafmt-badge]: https://img.shields.io/badge/code_style-scalafmt-c22d40.svg
[scalafmt-link]:  https://scalameta.org/scalafmt/
[travis-badge]:   https://travis-ci.org/clintval/cvbio.svg?branch=master
[travis-link]:    https://travis-ci.org/clintval/cvbio

Artisanal 🤣 bioinformatics Scala tools and pipelines.

## Development Workflow

First install [Mill][mill-link] and optionally create [IntelliJ][intellij-link] configuration files.

[intellij-link]: https://www.jetbrains.com/idea/download/#section=mac
[mill-link]:     https://github.com/lihaoyi/mill

```bash
❯ brew install mill
❯ mill mill.scalalib.GenIdea/idea
```

Then, assembly portable JARs with:

```bash
❯ mill _.localJar
```

If the above was successful, then you will find two JARs at `jars/`:

```
❯ ls -1 jars
cvbio-pipelines.jar
cvbio.jar
```

## Documentation

[Disambiguation Benchmarks](https://github.com/clintval/cvbio/blob/master/docs/benchmarks/disambiguate.md)
