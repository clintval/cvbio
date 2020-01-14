# cvbio

[![install with bioconda][bioconda-badge]][bioconda-link]
[![Build Status][travis-badge]][travis-link]
[![Code Coverage][codecov-badge]][codecov-link]
[![Language][scala-badge]][scala-link]
[![Code Style][scalafmt-badge]][scalafmt-link]
[![License][license-badge]][license-link]
[![Anaconda Version][anaconda-badge]][bioconda-link]

[anaconda-badge]: https://anaconda.org/bioconda/cvbio/badges/version.svg
[bioconda-badge]: https://img.shields.io/badge/install%20with-bioconda-brightgreen.svg
[bioconda-link]:  http://bioconda.github.io/recipes/cvbio/README.html
[codecov-badge]:  https://codecov.io/gh/clintval/cvbio/branch/master/graph/badge.svg
[codecov-link]:   https://codecov.io/gh/clintval/cvbio
[license-badge]:  https://img.shields.io/badge/license-MIT-blue.svg
[license-link]:   https://github.com/clintval/cvbio/blob/master/LICENSE
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

## Disambiguate

Disambiguate reads that were mapped to multiple references.

Disambiguation of aligned reads is performed per-template and all information across primary, secondary, and supplementary alignments is used as evidence.
Alignment disambiguation is commonly used when analyzing sequencing data from transduction, transfection, transgenic, or xenographic (including patient derived xenograft) experiments.
This tool works by comparing various alignment scores between a template that has been aligned to many references in order to determine which reference is the most likely source.

All templates which are positively assigned to a single source reference are written to a reference-specific output BAM file.
Any templates with ambiguous reference assignment are written to an ambiguous input-specific output BAM file.
Only BAMs produced from the Burrows-Wheeler Aligner (bwa) or STAR are currently supported.

Input BAMs of arbitrary sort order are accepted, however, an internal sort to queryname will be performed unless the BAM is already in queryname sort order.
All output BAM files will be written in the same sort order as the input BAM files.
Although paired-end reads will give the most discriminatory power for disambiguation of short-read sequencing data, this tool accepts paired, single-end (fragment), and mixed pairing input data.

#### Features

- Accepts SAM/BAM sources of any sort order
- Will disambiguate an arbitrary number of BAMs, all aligned to different references
- Writes the ambiguous alignments to an ambiguous-alignment specific directory
- Extensible implementation which supports alternative disambiguation strategies
- Early benchmarks show extremely high accuracy: [Click Here](https://github.com/clintval/cvbio/blob/master/docs/benchmarks/disambiguate.md)

#### Command Line Usage

```console
❯ cvbio Disambiguate -i infile1.bam infile2.bam -p insilico/disambiguated
```

#### Example

To disambiguate templates that are aligned to human (A) and mouse (B):

```console
❯ cvbio Disambiguate -i sample.A.bam sample.B.bam -p sample/sample -n hg38 mm10

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
```

## IgvBoss

Take control of your IGV session from end-to-end.

If no inputs are provided, then no new sessions will be created.
Adding multiple IGV-valid locus identifiers will result in a split-window view.
You must have already configured your IGV application to allow HTTPS connections over a port.
Enable remote control through the Advanced Tab of the Preferences Window in IGV.

#### IGV Startup

IGV will be initialized using the ordered logic:

  1. Let this tool connect to an already-running IGV session
  2. Supply an IGV JAR file path and let this tool run the JAR
  3. If you're on MacOS and have the Mac Application installed, IgvBoss will run it
  4. Finally, IgvBoss will attempt to find the 'igv' executable on the system path and run it

IgvBoss will always attempt to connect to a running IGV application before attempting to start a new instance of IGV.
Provide a path to an IGV JAR file if no IGV applications are currently running.
If no IGV JAR file path is set, and there are no running instances of IGV, then IgvBoss will attempt to fnd a locally installed version of IGV and run it.
If you are executing IgvBoss on a MacOS system, then IgvBoss will first look for an installed IGV Mac application.
If one cannot be found, or you're on a different operating system, then IgvBoss will search for an `igv` executable on the system path to execute.

#### IGV Shutdown

You can shutdown IGV on exit with the `--close-on-exit` option.
This will work regardless of how this tool initially connected to IGV and is handy for tearing down the application after your investigation is concluded.


#### Features

- Will start IGV for you if it's not already running
- Quick syntax to navigate IGV from the commandline only
- Easily re-load new files, travel to loci, and swap genomes.
- Shut IGV down with a single command `cvbio IgvBoss -x`

#### Command Line Usage

Load a BAM and interval list file into a new IGV session against the `mm10` on-disk genome.
Then go to two loci by name that are referenced in the first two name fields of the interval list.

```console
❯ cvbio IgvBoss -g mm10.fa -i infile.bam targets.bed -l $(cut -f4 < targets.bed | head -n2)
```

## UpdateContigNames

Update contig names in delimited data using a name mapping table.

A collection of mapping tables is maintained at the following location:

  * https://github.com/dpryan79/ChromosomeMappings

#### Features

- Optionally drop rows which have chromosome names not in the mapping file
- Replace multiple fields in a row at once using the same mapping file
- Directly write-out rows that start with arbitrary strings (default of `#`)
- Parses any delimited data using any single character delimiter

#### Command Line Usage

Relabel the contig names in an Ensembl human gene annotation file.

```console
❯ git clone https://github.com/dpryan79/ChromosomeMappings.git
❯ wget ftp://ftp.ensembl.org/pub/release-96/gtf/homo_sapiens/Homo_sapiens.GRCh38.96.gtf.gz

❯ cvbio UpdateContigNames \
    -i Homo_sapiens.GRCh38.96.gtf.gz \
    -o Homo_sapiens.GRCh38.96.ucsc-named.gtf.gz \
    -m ChromosomeMappings/GRCh38_ensembl2UCSC.txt \
    --comment-chars '#' \
    --columns 0 \
    --skip-missing false
```

<br>

---

## Testimonials

<br>

> "When [@clintval](https://github.com/clintval) saw the breadth of his repos he wept, for there were no more tools to put into scala."
>
> — [@ksebby](https://github.com/ksebby)
