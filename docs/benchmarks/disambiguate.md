### Disambiguation Micro-Benchmark

#### Introduction

To test our approach in a simple way, we choose a homologous gene that is present within human and murine species.
Our choice was guided by nothing other than the fact that `ABL1` was the first gene that came to mind when we attemped to contrive a small test case.
For the sake of storytelling, we may imagine that a patient with acute myeloid leukemia has undergone a biopsy, and that biologic material is incorporated as a zenograft into a mouse model for personalized treatment discovery.
We want to understand the clonal evolution of the patient's genomes as they are grafted into the mouse while likely under selection from many trial therapeutics.
For this experiment we seek to use high-throughput next-generation-sequencing, but we have a problem!

The difficulty with sequencing gDNA sourced from patient-derived xenograft samples is that both human and mouse germlines are present and are likely to confound variant calling results.
We need a way to disambiugate all sequencing reads from such a sample.

To demonstrate our ability to disambiguate templates, we will simulate short-reads over the `ABL1` gene from three species and test our approach:

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

#### Simulation Training Data

First, we need to generate read pairs from our favorite short-read sequencing platform, Illumina.
For this benchmark, it was quickest for us to simulate this data although we hazard the reader to put too much weight into this approach.
A more robust benchmark of this tool should involve real sequencing data from all species.
The below shows how we can quickly simulate 1,000 paired-end reads with uniform coverage over the target genes in all three species.

```bash
❯ mkdir -p insilico
❯ parallel \
    'dwgsim -N 1000 -c 0 -z 42 -x <( echo {2} ) -P {1} \
        /pipeline/reference-data/references/{1}/{1}.fa \
        insilico/{1}' \
  :::   hs38DH mm10 rn6 \
  :::+ 'chr9 130713881 130887675' 'chr2 31688354 31807093' 'chr3 10041820 10145076'
```

#### Digitaly Mixing Training Data

Next, we will virtually mix all the reads into a single FASTQ pair.
The only way to disambiguate the reads from this file is by looking at the read name prefix which we have marked with the assembly name.
The read name prefix will allow us to backtrack and check our disambiguation results post-alignment.

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

We then align all mixed reads to all three reference genomes independently.
Although verbose, we follow the [GATK best practice][gatk-reference] alignment guide so that we can ensure the sequence dictionaries of the output BAMs are correct.

[gatk-refrence]: https://software.broadinstitute.org/gatk/best-practices/workflow?id=11165

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

Now we are ready to disambiguate our aligned templates!

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
We hope to expand upon this benchmark in the future, but already, it looks like a high performance method for read-pair template disambiguation of arbitrary species!
