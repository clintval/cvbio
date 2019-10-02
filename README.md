# cvbio

[![install with bioconda][bioconda-badge]][bioconda-link]
[![Build Status][travis-badge]][travis-link]
[![Code Coverage][codecov-badge]][codecov-link]
[![Language][scala-badge]][scala-link]
[![Code Style][scalafmt-badge]][scalafmt-link]
[![Releases][releases-badge]][releases-link]
[![License][license-badge]][license-link]


[bioconda-badge]: https://img.shields.io/badge/install%20with-bioconda-brightgreen.svg
[bioconda-link]:  http://bioconda.github.io/recipes/cvbio/README.html
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

Artisanal 🤣 bioinformatics tools and pipelines in Scala.

Install with the Conda package manager after setting up your [Bioconda channels](https://bioconda.github.io/user/install.html#set-up-channels):

```text
❯ conda install cvbio
```

---

### Disambiguate

#### Features

- Accepts SAM/BAM sources of any sort order
- Will disambiguate an arbitrary number of BAMs, all aligned to different references
- Writes the ambiguous alignments to an ambiguous-alignment specific directory
- Extensible implementation which supports alternative disambiguation strategies
- Early benchmarks show extremely high accuracy: [Click Here](https://github.com/clintval/cvbio/blob/master/docs/benchmarks/disambiguate.md)

#### Command Line Usage

```console
❯ java -jar cvbio.jar Disambiguate -i infile1.bam infile2.bam -p insilico/disambiguated
```

#### Long Tool Description

```text
Disambiguate
------------------------------------------------------------------------------------------------------------------------
Disambiguate reads that were mapped to multiple references.

Disambiguation of aligned reads is performed per-template and all information across primary, secondary, and
supplementary alignments is used as evidence. Alignment disambiguation is commonly used when analyzing sequencing data
from transduction, transfection, transgenic, or xenographic (including patient derived xenograft) experiments. This
tool works by comparing various alignment scores between a template that has been aligned to many references in order
to determine which reference is the most likely source.

All templates which are positively assigned to a single source reference are written to a reference-specific output BAM
file. Any templates with ambiguous reference assignment are written to an ambiguous input-specific output BAM file.
Only BAMs produced from the Burrows-Wheeler Aligner (bwa) or STAR are currently supported.

Input BAMs of arbitrary sort order are accepted, however, an internal sort to queryname will be performed unless the
BAM is already in queryname sort order. All output BAM files will be written in the same sort order as the input BAM
files. Although paired-end reads will give the most discriminatory power for disambiguation of short- read sequencing
data, this tool accepts paired, single-end (fragment), and mixed pairing input data.

Example
-------

To disambiguate templates that are aligned to human (A) and mouse (B):

  ❯ java -jar cvbio.jar Disambiguate -i sample.A.bam sample.B.bam -p sample/sample -n hg38 mm10

  ❯ tree sample/
    sample/
    ├── ambiguous-alignments/
    │  ├── sample.A.ambiguous.bai
    │  ├── sample.A.ambiguous.bam
    │  ├── sample.B.ambiguous.bai
    │  └── sample.B.ambiguous.bam
    ├── sample.hg38.bai
    ├── sample.hg38.bam
    ├── sample.mm10.bai
    └── sample.mm10.bam

Glossary
--------

  * MAPQ: A metric that tells you how confident you can be that a read comes from a reported mapping position.
  * AS: A metric that tells you how similar the read is to the reference sequence.
  * NM: A metric that measures the number of mismatches to the reference sequence (Hamming distance).

Prior Art
---------

  * Disambiguate (https://github.com/AstraZeneca-NGS/disambiguate) from AstraZeneca's NGS team
```

---

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
------------------------------------------------------------------------------------------------------------------------

cvbio Arguments:
------------------------------------------------------------------------------------------------------------------------
-h [[true|false]], --help[[=true|false]]
                              Display the help message. [Default: false].
--async-io[[=true|false]]     Use asynchronous I/O where possible, e.g. for SAM and BAM files. [Default:
                              false].
--version[[=true|false]]      Display the version number for this tool. [Default: false].
--compression=Int             Default GZIP compression level, BAM compression level. [Default: 5].
--tmp-dir=DirPath             Directory to use for temporary files. [Default:
                              /var/folders/0m/dkk9n32j7qd6xrmf1bhk28wr0000gp/T].
--log-level=LogLevel          Minimum severity log-level to emit. [Default: Info]. Options: Debug, Info,
                              Warning, Error, Fatal.

Available Sub-Commands:
------------------------------------------------------------------------------------------------------------------------
Ensembl:                              Tools for downloading and formatting Ensembl data.
    FetchEnsemblGtf                    Fetch a GTF file from the Ensembl web server.
------------------------------------------------------------------------------------------------------------------------
SAM/BAM:                              Tools for manipulating SAM, BAM, and related data.
    Disambiguate                       Disambiguate reads that were mapped to multiple references.
------------------------------------------------------------------------------------------------------------------------
```
