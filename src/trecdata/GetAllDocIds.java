
package trecdata;

import common.CommonVariables;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
public class GetAllDocIds {

    File indexFile;
    Path indexpath;
    String dumpPath;

    public GetAllDocIds() {
    }

    public GetAllDocIds(File indexFile, String dumpPath) {
        this.indexFile = indexFile;
        this.indexpath = indexFile.toPath();
        this.dumpPath = dumpPath;
    }

    
    public static void main(String[] args) throws IOException {

        GetAllDocIds obj = new GetAllDocIds(new File("/store/collections/indexed/trec678"), 
            "/home/dwaipayan/trec678.docid");
        System.out.println("Writing docId from index: "+obj.indexFile.getAbsolutePath()+
            " in: "+obj.dumpPath);
        obj.getAllDocIds();
    }

    /**
     * 
     * @throws IOException 
     */
    public void getAllDocIds() throws IOException {

        FileWriter fileWritter = new FileWriter(dumpPath);
        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexpath))) {
            int maxDoc = reader.maxDoc();
            for (int i = 0; i < maxDoc; i++) {
                Document d = reader.document(i);
//                System.out.println(d.get(CommonVariables.FIELD_ID));
                bufferWritter.write(d.get(CommonVariables.FIELD_ID)+"\n");
            }
        }
        catch(IOException e) {
            System.err.println("NoSuchDirectory at: "+indexpath.toAbsolutePath());
//            e.printStackTrace();
        }
        bufferWritter.close();

    }
}
