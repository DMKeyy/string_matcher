# String Matcher

A Java 17 deduplication tool for clustering similar company names from CSV input.

It combines text normalization, Lucene-based candidate generation, and a weighted similarity metric to group near-duplicate entities.

## Features

- Company-name normalization (suffix removal, abbreviation expansion, basic unicode replacement)
- Fast candidate search with Apache Lucene 3-gram indexing
- Weighted similarity scoring:
  - Jaro-Winkler: 65%
  - Levenshtein-based score: 35%
- Concurrent matching pipeline with configurable threshold and cluster size
- CSV export of detected duplicate clusters

## Tech Stack

- Java 17
- Maven
- Apache Commons Text
- OpenCSV
- Apache Lucene

## Project Structure

```text
src/main/java/string_matcher/
  Main.java
  algorithms/
    BusinessEntityNormalizer.java
    CombinedSimilarityMetric.java
    UnionFindClustering.java
  core/
    DeduplicationEngine.java
    CandidateGenerator.java
    SimilarityMetric.java
    ...
  infrastructure/
    CsvRecordAdapter.java
    CsvRecordWriter.java
    LuceneCandidateGenerator.java
```

## Prerequisites

- JDK 17+
- Maven 3.8+

## Build

```bash
mvn clean package
```

## Run In Terminal

### Windows PowerShell

```powershell
cd C:\Users\ANIS\Desktop\Projects\Java_string_matcher\stringmatcher
mvn clean compile
mvn exec:java '-Dexec.mainClass=string_matcher.Main'
```

### Any Terminal (from project root)

```bash
mvn clean package
mvn exec:java -Dexec.mainClass=string_matcher.Main
```

### Run from IDE

Run `string_matcher.Main` as a Java application.

## Runtime Configuration

When the application starts, it shows an interactive menu:

1. Customize settings
2. Execute with current settings

Default values:

- Input path: `data/companyNamesLarge.csv`
- Output path: `data/clustered_companies.csv`
- Similarity threshold: `0.98`
- Max cluster size: `30`

## Input Format

Input is read from CSV. Each row is treated as one record.

Important behavior:

- If a row has multiple columns, they are joined with commas and treated as one string.
- There is no header skip logic by default.

Example input (`data/companyNamesLarge.csv`):

```csv
Acme GmbH
ACME Gesellschaft mit beschraenkter Haftung
Acme Inc.
Globex LLC
Globex Limited
```

## Output Format

Output CSV contains only clusters with more than one member.

Header:

```csv
ClusterId,OriginalString
```

Example output:

```csv
ClusterId,OriginalString
1,Acme GmbH
1,ACME Gesellschaft mit beschraenkter Haftung
1,Acme Inc.
2,Globex LLC
2,Globex Limited
```

## How Matching Works

1. Read and normalize records.
2. Build an in-memory Lucene index on normalized strings.
3. For each record, fetch up to 100 Lucene candidates.
4. Filter obvious length mismatches.
5. Score with combined metric and union matched records.
6. Refine clusters around a centroid to reduce weak links.
7. Write duplicate clusters to CSV.

## Tuning Tips

- Increase threshold (for example, `0.99`) to reduce false positives.
- Decrease threshold (for example, `0.95`) to catch more variations.
- Lower max cluster size to prevent over-merged groups in noisy data.
- Use cleaner input data (consistent naming and encoding) for better quality.

## Notes

- This project currently has no dedicated unit tests under `src/test/java`.
- The normalizer contains hard-coded unicode replacement rules and business suffix lists; adapt them for your locale/domain.
- `CsvRecordWriter` excludes clusters larger than 50 records from output as a safety guard.

## Data Files in This Repository

- `data/companyNamesLarge.csv`: sample input
- `data/clustered_companies.csv`: generated output

## License

No license file is currently included. Add one if you plan to distribute this project.
