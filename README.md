# cvbio

[![Build Status][travis-badge]][travis-link]
[![Language][scala-badge]][scala-link]
[![Code Style][scalafmt-badge]][scalafmt-link]
[![Releases][releases-badge]][releases-link]
[![License][license-badge]][license-link]

Artisanal 🤣 bioinformatics Scala tools and pipelines.

## Development Workflow

First install [Mill][mill-link] and optionally create [IntelliJ][intellij-link] configuration files.

```bash
❯ brew install mill
❯ cd cvbio && mill mill.scalalib.GenIdea/idea
```

Assemble portable JAR files with:

```bash
❯ mill _.deployLocal
❯ ls -1 jars
cvbio-pipelines.jar
cvbio.jar
```

## Examples

### Streaming an Ensembl GTF

```bash
❯ java -jar jars/cvbio.jar FetchEnsemblGtf --species "Homo sapiens" --build 38 --release 96 2> /dev/null | head 
#!genome-build GRCh38.p12
#!genome-version GRCh38
#!genome-date 2013-12
#!genome-build-accession NCBI:GCA_000001405.27
#!genebuild-last-updated 2018-11
1       havana  gene    11869   14409   .       +       .       gene_id "ENSG00000223972"; gene_version "5"; gene_name "DDX11L1"; gene_source "havana"; gene_biotype "transcribed_unprocessed_pseudogene";
1       havana  transcript      11869   14409   .       +       .       gene_id "ENSG00000223972"; gene_version "5"; transcript_id "ENST00000456328"; transcript_version "2"; gene_name "DDX11L1"; gene_source "havana"; gene_biotype "transcribed_unprocessed_pseudogene"; transcript_name "DDX11L1-202"; transcript_source "havana"; transcript_biotype "processed_transcript"; tag "basic"; transcript_support_level "1";
1       havana  exon    11869   12227   .       +       .       gene_id "ENSG00000223972"; gene_version "5"; transcript_id "ENST00000456328"; transcript_version "2"; exon_number "1"; gene_name "DDX11L1"; gene_source "havana"; gene_biotype "transcribed_unprocessed_pseudogene"; transcript_name "DDX11L1-202"; transcript_source "havana"; transcript_biotype "processed_transcript"; exon_id "ENSE00002234944"; exon_version "1"; tag "basic"; transcript_support_level "1";
1       havana  exon    12613   12721   .       +       .       gene_id "ENSG00000223972"; gene_version "5"; transcript_id "ENST00000456328"; transcript_version "2"; exon_number "2"; gene_name "DDX11L1"; gene_source "havana"; gene_biotype "transcribed_unprocessed_pseudogene"; transcript_name "DDX11L1-202"; transcript_source "havana"; transcript_biotype "processed_transcript"; exon_id "ENSE00003582793"; exon_version "1"; tag "basic"; transcript_support_level "1";
1       havana  exon    13221   14409   .       +       .       gene_id "ENSG00000223972"; gene_version "5"; transcript_id "ENST00000456328"; transcript_version "2"; exon_number "3"; gene_name "DDX11L1"; gene_source "havana"; gene_biotype "transcribed_unprocessed_pseudogene"; transcript_name "DDX11L1-202"; transcript_source "havana"; transcript_biotype "processed_transcript"; exon_id "ENSE00002312635"; exon_version "1"; tag "basic"; transcript_support_level "1";
```

### Preparing Reference Data for `STAR`

```bash
❯ java -jar jars/cvbio-pipelines.jar PrepareStarReferenceData \
    -i references/hg38.fa \
    -g transcripts.gtf \
    -o STAR-references/hg38 \
    --overhang 75 \
    --threads 8
```

### Aligning a BAM File with `STAR`

```bash
❯ java -jar jars/cvbio-pipelines.jar StarAlignPipeline \
    -i input.bam \
    -g STAR-references/hg38 \
    -p output/sample.library \
    -r references/hg38.fa \
    --two-pass Basic
```

[license-badge]:           http://img.shields.io/badge/license-MIT-blue.svg
[license-link]:            https://github.com/clintval/cvbio/blob/master/LICENSE
[releases-badge]:          https://img.shields.io/badge/cvbio_Releases-555555.svg
[releases-link]:           https://github.com/clintval/cvbio/releases
[scala-badge]:             https://img.shields.io/badge/language-scala-c22d40.svg
[scala-link]:              https://www.scala-lang.org/
[scalafmt-badge]:          https://img.shields.io/badge/code_style-scalafmt-c22d40.svg
[scalafmt-link]:           https://scalameta.org/scalafmt/
[travis-badge]:            https://travis-ci.org/clintval/cvbio.svg?branch=master
[travis-link]:             https://travis-ci.org/clintval/cvbio

[intellij-link]: https://www.jetbrains.com/idea/download/#section=mac
[mill-link]:     https://github.com/lihaoyi/mill
