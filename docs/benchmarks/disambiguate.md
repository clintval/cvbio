### Disambiguation Micro-Benchmark

#### Introduction

As a preliminary test, we will attempt to disambiguate simulated paired-end reads from a homologous gene (`ABL1`) across human and murine species.

| Assembly   | Species             | Coordinates                                    |
| ---        | ---                 | ---                                            |
| `hg38`     | _Homo sapiens_      | [`chr9:130713881-130887675`][hs38DH-reference] |
| `mm10`     | _Mus musculus_      | [`chr2:31688354-31807093`][mm10-reference]     |
| `rn6`      | _Rattus norvegicus_ | [`chr3:10041820-10145076`][rn6-reference]      |
> Location of the `ABL1` gene in three model species.

[hs38DH-reference]: https://rgd.mcw.edu/rgdweb/report/gene/main.html?id=1342972
[mm10-reference]:   https://rgd.mcw.edu/rgdweb/report/gene/main.html?id=1332211
[rn6-reference]:    https://rgd.mcw.edu/rgdweb/report/gene/main.html?id=1584969

#### Dependencies

- [`bwa`](https://bioconda.github.io/recipes/bwa/README.html)
- [`dwgsim`](https://anaconda.org/bioconda/dwgsim)
- [`parallel`](https://anaconda.org/conda-forge/parallel)
- [`picard`](https://bioconda.github.io/recipes/picard/README.html)

#### Simulation of Training Data

We will simulate 1,000 paired-end reads with uniform coverage over the target gene in all three species.
For the sake of tracking read pairs, we use the `-P` flag and prefix each read name with the source assembly ID.

```bash
❯ mkdir -p insilico
❯ parallel \
    'dwgsim -N 1000 -c 0 -z 42 -x <( echo {2} ) -P {1} \
         /pipeline/reference-data/references/{1}/{1}.fa \
         insilico/{1}' \
    ::: hs38DH mm10 rn6 \
    :::+ 'chr9 130713881 130887675' 'chr2 31688354 31807093' 'chr3 10041820 10145076'
```

#### Digitaly Mixing All Training Data

We concatenate all reads into a single FASTQ pair and then convert that file into an umapped BAM (uBAM) file.

```bash
❯ cat insilico/*read1* > insilico/all.read1.fastq
❯ cat insilico/*read2* > insilico/all.read2.fastq
❯ picard FastqToSam \
    F1=insilico/all.read1.fastq \
    F2=insilico/all.read2.fastq  \
    OUTPUT=insilico/all.unmapped.bam \
    SAMPLE_NAME=all 
```

#### Alignment

We align all read pairs to all three reference genomes independently.
Although verbose, we follow the [GATK best practice][gatk-reference] alignment guide so that we ensure the sequence dictionaries of the output BAMs are correct.

[gatk-reference]: https://software.broadinstitute.org/gatk/best-practices/workflow?id=11165

```bash
❯ parallel \
    'picard SamToFastq \
         INPUT=insilico/all.unmapped.bam \
         INTERLEAVE=true \
         FASTQ=/dev/stdout \
     | bwa mem -t 8 -p \
         /pipeline/reference-data/references/{}/{}.fa \
         /dev/stdin \
     | picard MergeBamAlignment \
         ALIGNED=/dev/stdin \
         UNMAPPED=insilico/all.unmapped.bam \
         OUTPUT=/dev/stdout \
         REFERENCE_SEQUENCE=/pipeline/reference-data/references/{}/{}.fa \
     | picard SortSam \
         INPUT=/dev/stdin \
         OUTPUT=insilico/all.{}.mapped.bam \
         SORT_ORDER=queryname' \
  ::: hs38DH mm10 rn6
```

#### Disambiguation

We are now ready to disambiguate our aligned templates!

```bash
❯ java -jar cvbio.jar Disambiguate -i insilico/all.*.mapped.bam -o insilico/disambiguated
```

#### Validation

Let's see how we did:

```bash
❯ \ls -1 insilico/disambiguated*
insilico/disambiguated.hs38DH.bam
insilico/disambiguated.mm10.bam
insilico/disambiguated.rn6.bam
```

```bash
❯ parallel -k \
    'echo Distribution of known alignments disambiguated into: {} \
     && picard ViewSam \
          INPUT=insilico/disambiguated.{}.bam \
          ALIGNMENT_STATUS=All \
          PF_STATUS=All \
        2>/dev/null \
        | grep -v "@" \
        | tr "_" "\t" \
        | cut -f1 \
        | sort \
        | uniq -c \
     && echo' \
    ::: hs38DH mm10 rn6

Distribution of known alignments disambiguated into: hs38DH
1898 hs38DH

Distribution of known alignments disambiguated into: mm10
1906 mm10
   6 rn6

Distribution of known alignments disambiguated into: rn6
   2 mm10
1914 rn6
```

#### Conclusion

Because these are alignments, and not read pairs or templates, we cannot directly calculate performance metrics such as sensitivity or specificty of this method.
We hope to improve these benchmarks.