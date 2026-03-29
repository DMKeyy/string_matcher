package string_matcher.infrastructure;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import string_matcher.core.CandidateGenerator;
import string_matcher.core.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuceneCandidateGenerator implements CandidateGenerator {
    private Directory directory;
    private final Analyzer analyzer;
    private IndexReader reader;
    private IndexSearcher searcher;
    private Map<Integer, Record> recordMap;

    public LuceneCandidateGenerator() {
        this.analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                WhitespaceTokenizer src = new WhitespaceTokenizer();
                NGramTokenFilter filter = new NGramTokenFilter(src, 3, 3, true);
                return new TokenStreamComponents(src, filter);
            }
        };
    }

    @Override
    public void buildIndex(List<Record> records) throws Exception {
        directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        recordMap = new HashMap<>();
        
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            for (Record record : records) {
                recordMap.put(record.id(), record);
                
                Document doc = new Document();
                doc.add(new StringField("id", String.valueOf(record.id()), Field.Store.YES));
                doc.add(new TextField("normalized", record.normalizedString(), Field.Store.YES));
                writer.addDocument(doc);
            }
        }
        
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
    }

    @Override
    public List<Record> findCandidates(Record queryRecord, int maxCandidates) throws Exception {
        if (queryRecord.normalizedString().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Escape special lucene characters
        String escaped = QueryParser.escape(queryRecord.normalizedString());
        
        QueryParser parser = new QueryParser("normalized", analyzer);
        parser.setDefaultOperator(QueryParser.Operator.OR);
        Query query = parser.parse(escaped);
        
        TopDocs topDocs = searcher.search(query, maxCandidates);
        List<Record> candidates = new ArrayList<>();
        
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.storedFields().document(scoreDoc.doc);
            int id = Integer.parseInt(doc.get("id"));
            if (id != queryRecord.id()) {
                candidates.add(recordMap.get(id));
            }
        }
        
        return candidates;
    }
}