package test.lucene;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;

public class LuceneTest extends TestCase {
  //创建索引
  public void testCreateIndex() throws IOException {
    //指定索引库的存放位置Directory对象
    Directory directory = FSDirectory.open(new File("./data/lucene/index"));
    //索引库还可以存放到内存中
    //Directory directory = new RAMDirectory();

    //指定一个标准分析器，对文档内容进行分析
    Analyzer analyzer = new IKAnalyzer();

    //创建indexwriterCofig对象
    //第一个参数： Lucene的版本信息，可以选择对应的lucene版本也可以使用LATEST
    //第二根参数：分析器对象
    IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);

    //创建一个indexwriter对象
    IndexWriter indexWriter = new IndexWriter(directory, config);

    //原始文档的路径
    File file = new File("./data/lucene/searchsource");
    File[] fileList = file.listFiles();
    for (File file2 : fileList) {
      //创建document对象
      Document document = new Document();

      //创建field对象，将field添加到document对象中

      //文件名称
      String fileName = file2.getName();
      //创建文件名域
      //第一个参数：域的名称
      //第二个参数：域的内容
      //第三个参数：是否存储
      Field fileNameField = new TextField("fileName", fileName, Field.Store.YES);

      //文件的大小
      long fileSize  = FileUtils.sizeOf(file2);
      //文件大小域
      Field fileSizeField = new LongField("fileSize", fileSize, Field.Store.YES);

      //文件路径
      String filePath = file2.getPath();
      //文件路径域（不分析、不索引、只存储）
      Field filePathField = new StoredField("filePath", filePath);

      //文件内容
      String fileContent = FileUtils.readFileToString(file2);
      //String fileContent = FileUtils.readFileToString(file2, "utf-8");
      //文件内容域
      Field fileContentField = new TextField("fileContent", fileContent, Field.Store.YES);

      document.add(fileNameField);
      document.add(fileSizeField);
      document.add(filePathField);
      document.add(fileContentField);
      //使用indexwriter对象将document对象写入索引库，此过程进行索引创建。并将索引和document对象写入索引库。
      indexWriter.addDocument(document);
    }
    //关闭IndexWriter对象。
    indexWriter.close();
  }

  protected String indexDirectory="./data/lucene/index";
  public void testMatchAllDocsQuery() throws Exception {
    //创建一个Directory对象，指定索引库存放的路径
    Directory directory = FSDirectory.open(new File(indexDirectory));
    //创建IndexReader对象，需要指定Directory对象
    IndexReader indexReader = DirectoryReader.open(directory);
    //创建Indexsearcher对象，需要指定IndexReader对象
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);

    //创建查询条件
    //使用MatchAllDocsQuery查询索引目录中的所有文档
    Query query = new MatchAllDocsQuery();
    //执行查询
    //第一个参数是查询对象，第二个参数是查询结果返回的最大值
    TopDocs topDocs = indexSearcher.search(query, 10);

    //查询结果的总条数
    System.out.println("查询结果的总条数："+ topDocs.totalHits);
    //遍历查询结果
    //topDocs.scoreDocs存储了document对象的id
    //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
    print(indexSearcher, topDocs);
    //关闭indexreader对象
    indexReader.close();
  }

  private void print(IndexSearcher indexSearcher, TopDocs topDocs)
      throws IOException {
    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      //scoreDoc.doc属性就是document对象的id
      //int doc = scoreDoc.doc;
      //根据document的id找到document对象
      Document document = indexSearcher.doc(scoreDoc.doc);
      //文件名称
      System.out.println(document.get("fileName"));
      //文件内容
      System.out.println(document.get("fileContent"));
      //文件大小
      System.out.println(document.get("fileSize"));
      //文件路径
      System.out.println(document.get("filePath"));
      System.out.println("----------------------------------");
    }
  }

  //搜索索引
  public void testSearchIndex() throws IOException{
    //创建一个Directory对象，指定索引库存放的路径
    Directory directory = FSDirectory.open(new File(indexDirectory));
    //创建IndexReader对象，需要指定Directory对象
    IndexReader indexReader = DirectoryReader.open(directory);
    //创建Indexsearcher对象，需要指定IndexReader对象
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    //创建一个TermQuery（精准查询）对象，指定查询的域与查询的关键词
    //创建查询
    Query query = new TermQuery(new Term("fileName", "lucene"));
    //执行查询
    //第一个参数是查询对象，第二个参数是查询结果返回的最大值
    TopDocs topDocs = indexSearcher.search(query, 10);
    //查询结果的总条数
    System.out.println("查询结果的总条数："+ topDocs.totalHits);
    //遍历查询结果
    //topDocs.scoreDocs存储了document对象的id
    //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
    print(indexSearcher, topDocs);
    //关闭indexreader对象
    indexReader.close();
  }
  //数值范围查询
  public void testNumericRangeQuery() throws Exception {
    //创建一个Directory对象，指定索引库存放的路径
    Directory directory = FSDirectory.open(new File(indexDirectory));
    //创建IndexReader对象，需要指定Directory对象
    IndexReader indexReader = DirectoryReader.open(directory);
    //创建Indexsearcher对象，需要指定IndexReader对象
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);

    //创建查询
    //参数：
    //1.域名
    //2.最小值
    //3.最大值
    //4.是否包含最小值
    //5.是否包含最大值
    Query query = NumericRangeQuery.newLongRange("fileSize", 390L, 399L, true, true);
    //执行查询

    //第一个参数是查询对象，第二个参数是查询结果返回的最大值
    TopDocs topDocs = indexSearcher.search(query, 10);

    //查询结果的总条数
    System.out.println("查询结果的总条数："+ topDocs.totalHits);
    //遍历查询结果
    //topDocs.scoreDocs存储了document对象的id
    //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
    print(indexSearcher, topDocs);
    //关闭indexreader对象
    indexReader.close();
  }

  //组合条件查询
  public void testBooleanQuery() throws Exception {
    //创建一个Directory对象，指定索引库存放的路径
    Directory directory = FSDirectory.open(new File(indexDirectory));
    //创建IndexReader对象，需要指定Directory对象
    IndexReader indexReader = DirectoryReader.open(directory);
    //创建Indexsearcher对象，需要指定IndexReader对象
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);

    //创建一个布尔查询对象
    BooleanQuery query = new BooleanQuery();
    //创建第一个查询条件
    Query query1 = new TermQuery(new Term("fileName", "apache"));
    Query query2 = new TermQuery(new Term("fileName", "lucene"));
    //组合查询条件
    query.add(query1, BooleanClause.Occur.SHOULD);
    query.add(query2, BooleanClause.Occur.SHOULD);
    //执行查询

    //第一个参数是查询对象，第二个参数是查询结果返回的最大值
    TopDocs topDocs = indexSearcher.search(query, 10);

    //查询结果的总条数
    System.out.println("查询结果的总条数："+ topDocs.totalHits);
    //遍历查询结果
    //topDocs.scoreDocs存储了document对象的id
    //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
    print(indexSearcher, topDocs);
    //关闭indexreader对象
    indexReader.close();
  }

  public void testQueryParser() throws Exception {
    //创建一个Directory对象，指定索引库存放的路径
    Directory directory = FSDirectory.open(new File(indexDirectory));
    //创建IndexReader对象，需要指定Directory对象
    IndexReader indexReader = DirectoryReader.open(directory);
    //创建Indexsearcher对象，需要指定IndexReader对象
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);

    //创建queryparser对象
    //第一个参数默认搜索的域
    //第二个参数就是分析器对象
    QueryParser queryParser = new QueryParser("fileName", new IKAnalyzer());
    //使用默认的域,这里用的是语法，下面会详细讲解一下
//    Query query = queryParser.parse("lucene");
    //不使用默认的域，可以自己指定域
    Query query = queryParser.parse("fileContent:Version");
    //执行查询


    //第一个参数是查询对象，第二个参数是查询结果返回的最大值
    TopDocs topDocs = indexSearcher.search(query, 10);

    //查询结果的总条数
    System.out.println("查询结果的总条数："+ topDocs.totalHits);
    //遍历查询结果
    //topDocs.scoreDocs存储了document对象的id
    //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
    print(indexSearcher, topDocs);
    //关闭indexreader对象
    indexReader.close();
  }

  public void testMultiFiledQueryParser() throws Exception {
    //创建一个Directory对象，指定索引库存放的路径
    Directory directory = FSDirectory.open(new File(indexDirectory));
    //创建IndexReader对象，需要指定Directory对象
    IndexReader indexReader = DirectoryReader.open(directory);
    //创建Indexsearcher对象，需要指定IndexReader对象
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);

    //可以指定默认搜索的域是多个
    String[] fields = {"fileName", "fileContent"};
    //创建一个MulitFiledQueryParser对象
    MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fields, new IKAnalyzer());
    Query query = queryParser.parse("apache");
    System.out.println(query);
    //执行查询


    //第一个参数是查询对象，第二个参数是查询结果返回的最大值
    TopDocs topDocs = indexSearcher.search(query, 10);

    //查询结果的总条数
    System.out.println("查询结果的总条数："+ topDocs.totalHits);
    //遍历查询结果
    //topDocs.scoreDocs存储了document对象的id
    //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
    print(indexSearcher, topDocs);
    //关闭indexreader对象
    indexReader.close();
  }

  //删除全部索引
  public void testDeleteAllIndex() throws Exception {
    Directory directory = FSDirectory.open(new File(indexDirectory));
    Analyzer analyzer = new IKAnalyzer();
    IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
    IndexWriter indexWriter = new IndexWriter(directory, config);
    //删除全部索引
    indexWriter.deleteAll();
    //关闭indexwriter
    indexWriter.close();
  }

  //根据查询条件删除索引
  public void testDeleteIndexByQuery() throws Exception {
    Directory directory = FSDirectory.open(new File(indexDirectory));
    Analyzer analyzer = new IKAnalyzer();
    IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
    IndexWriter indexWriter = new IndexWriter(directory, config);
    //创建一个查询条件
    Query query = new TermQuery(new Term("fileName", "apache"));
    //根据查询条件删除
    indexWriter.deleteDocuments(query);
    //关闭indexwriter
    indexWriter.close();
  }

  //修改索引库
  public void testUpdateIndex() throws Exception {
    Directory directory = FSDirectory.open(new File(indexDirectory));
    Analyzer analyzer = new IKAnalyzer();
    IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
    IndexWriter indexWriter = new IndexWriter(directory, config);
    //创建一个Document对象
    Document document = new Document();
    //向document对象中添加域。
    //不同的document可以有不同的域，同一个document可以有相同的域。
    document.add(new TextField("fileName", "要更新的文档", Field.Store.YES));
    document.add(new TextField("fileContent", "简介 Lucene 是一个基于 Java 的全文信息检索工具包。", Field.Store.YES));
    indexWriter.updateDocument(new Term("fileName", "lucene"), document);
    //关闭indexWriter
    indexWriter.close();
  }
}
