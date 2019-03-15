package test;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/8/29 0029.
 * Lucene 索引管理工具类
 */
public class IndexManagerUtils {

  /**
   * 为指定目录下的文件创建索引,包括其下的所有子孙目录下的文件
   *
   * @param targetFileDir ：需要创建索引的文件目录
   * @param indexSaveDir  ：创建好的索引保存目录
   * @throws IOException
   */
  public static void indexCreate(File targetFileDir, File indexSaveDir) throws IOException {
    /** 如果传入的路径不是目录或者目录不存在，则放弃*/
    if (!targetFileDir.isDirectory() || !targetFileDir.exists()) {
      return;
    }

    /** 创建 Lucene 文档列表，用于保存多个 Docuemnt*/
    List<Document> docList = new ArrayList<Document>();

    /**循环目标文件夹，取出文件
     * 然后获取文件的需求内容，添加到 Lucene 文档(Document)中
     * 此例会获取 文件名称、文件内容、文件大小
     * */
    for (File file : targetFileDir.listFiles()) {
      if (file.isDirectory()) {
        /**如果当前是目录，则进行方法回调*/
        indexCreate(file, indexSaveDir);
      } else {
        /**如果当前是文件，则进行创建索引*/
        /** 文件名称：如  abc.txt*/
        String fileName = file.getName();

        /**文件内容：org.apache.commons.io.FileUtils 操作文件更加方便
         * readFileToString：直接读取整个文本文件内容*/
        String fileContext = FileUtils.readFileToString(file);

        /**文件大小：sizeOf，单位为字节*/
        Long fileSize = FileUtils.sizeOf(file);

        /**Lucene 文档对象(Document)，文件系统中的一个文件就是一个 Docuemnt对象
         * 一个 Lucene Docuemnt 对象可以存放多个 Field（域）
         *  Lucene Docuemnt 相当于 Mysql 数据库表的一行记录
         *  Docuemnt 中 Field 相当于 Mysql 数据库表的字段*/
        Document luceneDocument = new Document();

        /**
         * TextField 继承于 org.apache.lucene.document.Field
         * TextField(String name, String value, Store store)--文本域
         *  name：域名，相当于 Mysql 数据库表的字段名
         *  value：域值，相当于 Mysql 数据库表的字段值
         *  store：是否存储，yes 表存储，no 为不存储
         *
         * 默认所有的 Lucene 文档的这三个域的内容都会进行分词，创建索引目录，后期可以根据这个三个域来进行检索
         * 如查询 文件名(fileName) 包含 "web" 字符串的文档
         * 查询 文件内容(fileContext) 包含 "spring" 字符串的文档
         * 查询 文件大小(fileSize) 等于 2055 字节的文档 等等
         *
         * TextField：表示文本域、默认会分词、会创建索引、第三个参数 Store.YES 表示会存储
         * 同理还有 StoredField、StringField、FeatureField、BinaryDocValuesField 等等
         * 都来自于超级接口：org.apache.lucene.index.IndexableField
         */
        TextField nameFiled = new TextField("fileName", fileName, Store.YES);
        TextField contextFiled = new TextField("fileContext", fileContext, Store.YES);

        /**如果是 Srore.NO，则不会存储，就意味着后期获取 fileSize 值的时候，值会为null
         * 虽然 Srore.NO 不会存在域的值，但是 TextField本身会分词、会创建索引
         * 所以后期仍然可以根据 fileSize 域进行检索：queryParser.parse("fileContext:" + queryWord);
         * 只是获取 fileSize 存储的值为 null：document.get("fileSize"));
         * 索引是索引，存储的 fileSize 内容是另一回事
         * */
        TextField sizeFiled = new TextField("fileSize", fileSize.toString(), Store.YES);

        /**将所有的域都存入 Lucene 文档中*/
        luceneDocument.add(nameFiled);
        luceneDocument.add(contextFiled);
        luceneDocument.add(sizeFiled);

        /**将文档存入文档集合中，之后再同统一进行存储*/
        docList.add(luceneDocument);
      }
    }

    /** 创建分词器
     * StandardAnalyzer：标准分词器，对英文分词效果很好，对中文是单字分词，即一个汉字作为一个词，所以对中文支持不足
     * 市面上有很多好用的中文分词器，如 IKAnalyzer 就是其中一个
     * 现在换成 IKAnalyzer 中文分词器
     */
    /*Analyzer analyzer = new StandardAnalyzer();*/
    Analyzer analyzer = new IKAnalyzer();

    /**如果目录不存在，则会自动创建
     * FSDirectory：表示文件系统目录，即会存储在计算机本地磁盘，继承于
     * org.apache.lucene.store.BaseDirectory
     * 同理还有：org.apache.lucene.store.RAMDirectory：存储在内存中
     * Lucene 7.4.0 版本 open 方法传入的 Path 对象
     * Lucene 4.10.3 版本 open 方法传入的是 File 对象
     */
    Directory directory = FSDirectory.open(indexSaveDir);

    /** 创建 索引写配置对象，传入分词器
     * Lucene 7.4.0 版本 IndexWriterConfig 构造器不需要指定 Version.LUCENE_4_10_3
     * Lucene 4.10.3 版本 IndexWriterConfig 构造器需要指定 Version.LUCENE_4_10_3
     * */
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);

    /**创建 索引写对象，用于正式写入索引和文档数据*/
    IndexWriter indexWriter = new IndexWriter(directory, config);

    /**将 Lucene 文档加入到 写索引 对象中*/
    for (int i = 0; i < docList.size(); i++) {
      indexWriter.addDocument(docList.get(i));
    }
    /**最后再 刷新流，然后提交、关闭流
     * Lucene 4.10.3 在 close 的时候会自动 flush，程序员无法调用
     * Lucene 7.4.0 可以自己手动调用 flush 方法*/
    indexWriter.commit();
    indexWriter.close();
    indexWriter.close();
  }

  public static void main(String[] args) throws IOException {
    File file1 = new File("./data/searchsource");
    File file2 = new File("./data/luceneIndex");
    indexCreate(file1, file2);
  }
}

