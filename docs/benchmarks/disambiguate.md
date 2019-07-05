### Disambiguation Benchmark

As a proof of concept we choose a homologous gene within two murine species as well as in the human species.
I've chosen the `ABL1` gene which has undergone an incredible amount of research in human species as it is commonly mutated in leukemic disease.
My choice was guided by nothing other than the fact it was the first gene that came to mind when I attemped to contrive a small test case.

Location of the ABL1 gene in three model species:

| Assembly   | Species             | Coordinates                |
| ---        | ---                 | ---                        |
| `hg38`     | _Homo sapiens_      | [`chr9:130713881-130887675`][hs38DH-reference] |
| `mm10`     | _Mus musculus_      | [`chr2:31688354-31807093`][mm10-reference]     |
| `rn6`      | _Rattus norvegicus_ | [`chr3:10041820-10145076`][rn6-reference]      |

[hs38DH-reference]: https://rgd.mcw.edu/rgdweb/report/gene/main.html?id=1342972
[mm10-reference]:   https://rgd.mcw.edu/rgdweb/report/gene/main.html?id=1332211
[rn6-reference]:    https://rgd.mcw.edu/rgdweb/report/gene/main.html?id=1584969

Dependencies:

- [`bwa`](https://bioconda.github.io/recipes/bwa/README.html)
- [`dwgsim`](https://anaconda.org/bioconda/dwgsim)
- [`parallel`](https://anaconda.org/conda-forge/parallel)
- [`picard`](https://bioconda.github.io/recipes/picard/README.html)

First we need to read pairs from our favorite short-read sequencing platform, Illumina.
For this benchmark, it was quickest for me to simulate this data although I hazard the reader to put too much weight into this approach.
A more robust benchmark of this tool should involve real sequencing data from both species, and preferably over the entire genome or relevant genomic targets.
The below shows how we can quickly simulate 1,000x paired-end reads with uniform coverage over the target genes in all three species.

```bash
❯ mkdir -p insilico
❯ parallel \
    'dwgsim -N 1000 -c 0 -z 42 -x <( echo {2} ) -P {1} \
        /pipeline/reference-data/references/{1}/{1}.fa \
        insilico/{1}' \
  :::   hs38DH mm10 rn6 \
  :::+ 'chr9 130713881 130887675' 'chr2 31688354 31807093' 'chr3 10041820 10145076'
```

Let's virtually mix all the reads into a single FASTQ pair.
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

No we're ready to disambiguate are aligned templates!

```bash
❯ java -jar cvbio.jar Disambiguate -i insilico/all.*.mapped.bam -o insilico/disambiguated
```

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

Because these are alignments, and not read pairs or templates, we cannot directly calculate performance metrics such as sensitivity or specificty of this method.
I hope to expand upon this benchmark in the future, but already, it looks like a high performance method for read-pair template disambiguation of arbitrary species!
