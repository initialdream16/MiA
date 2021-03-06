/*
 * Source Code for Listing 10.5
 * 
 */
package mia.clustering.ch10;

import java.io.File;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.lucene.analysis.Analyzer;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.RandomSeedGenerator;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.distance.CosineDistanceMeasure;
import org.apache.mahout.vectorizer.DictionaryVectorizer;
import org.apache.mahout.vectorizer.DocumentProcessor;
import org.apache.mahout.vectorizer.tfidf.TFIDFConverter;

public class NewsKMeansClustering {
  
  public static void main(String args[]) throws Exception {
    
    int minSupport = 5;
    int minDf = 5;
    int maxDFPercent = 99;
    int maxNGramSize = 1;
    int minLLRValue = 50;
    int reduceTasks = 1;
    int chunkSize = 200;
    int norm = -1;
    boolean sequentialAccessOutput = true;
    
    String inputDir = "reuters-seqfiles";
    File inputDirFile = new File(inputDir);
    Configuration conf = new Configuration();
    FileSystem fs = FileSystem.get(conf);

    String outputDir = "newsClusters";
    HadoopUtil.delete(conf, new Path(outputDir));
    Path tokenizedPath = new Path(outputDir,
        DocumentProcessor.TOKENIZED_DOCUMENT_OUTPUT_FOLDER);
    MyAnalyzer analyzer = new MyAnalyzer();
    DocumentProcessor.tokenizeDocuments(new Path(inputDir), analyzer
        .getClass().asSubclass(Analyzer.class), tokenizedPath, conf);

    DictionaryVectorizer.createTermFrequencyVectors(tokenizedPath,
      new Path(outputDir), conf, minSupport, maxNGramSize, minLLRValue, 2, true, reduceTasks,
      chunkSize, sequentialAccessOutput, false);
    TFIDFConverter.processTfIdf(
      new Path(outputDir , DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER),
      new Path(outputDir), conf, chunkSize, minDf,
      maxDFPercent, norm, true, sequentialAccessOutput, false, reduceTasks);
    Path vectorsFolder = new Path(outputDir, "tfidf-vectors");
    Path centroids = new Path(outputDir, "centroids");
    Path clusterOutput = new Path(outputDir, "clusters");
    
    RandomSeedGenerator.buildRandom(conf, vectorsFolder, centroids, 20,
      new CosineDistanceMeasure());
    KMeansDriver.run(conf, vectorsFolder, centroids, clusterOutput,
      new CosineDistanceMeasure(), 0.01, 20, true, false);
    
    SequenceFile.Reader reader = new SequenceFile.Reader(fs,
        new Path(clusterOutput, Cluster.CLUSTERED_POINTS_DIR
                                + "/part-m-00000"), conf);
  }
}
