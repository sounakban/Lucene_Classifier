package CommonResources;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.io.FileWriter;


import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexableField;



/**
 *
 * @author sounakbanerjee
 */



public class DumpIndex {

    /**
     * Reads the index from the directory passed as argument or "index" if no
     * arguments are given.
     */
    public static void main(String[] args) throws Exception {
        String index = (args.length > 0 ? args[0] : "index");
        new DumpIndex(index).dump();
    }

    private String dir;

    public DumpIndex(String dir) {
        this.dir = dir;
    }

    public void dump() throws XMLStreamException, FactoryConfigurationError,
            CorruptIndexException, IOException {
        XMLStreamWriter out = XMLOutputFactory.newInstance()
                .createXMLStreamWriter(new FileWriter("/Users/sounakbanerjee/Desktop/Temp/XMLDump/dump.xml"));

        FSDirectory index = FSDirectory.open(Paths.get("/Users/sounakbanerjee/Desktop/Temp/index"));
        IndexReader reader = DirectoryReader.open(index);
        //out.writeStartDocument();
        out.writeStartDocument("utf-8", "1.0");
        out.writeCharacters("\n");
        out.writeStartElement("documents");
        out.writeCharacters("\n");
        for (int i = 0; i < reader.numDocs(); i++)
            dumpDocument(reader.document(i), out);
        out.writeEndElement();
        out.writeEndDocument();

        out.flush();
        reader.close();
    }

    @SuppressWarnings("unchecked")
    private void dumpDocument(Document document, XMLStreamWriter out)
            throws XMLStreamException {
        out.writeStartElement("document");
        for (IndexableField field : (List<IndexableField>) document.getFields()) {
            out.writeCharacters("\n");
            out.writeStartElement("field");
            out.writeAttribute("name", field.name());
            out.writeAttribute("value", field.stringValue());
            out.writeEndElement();
        }
        out.writeCharacters("\n");
        out.writeEndElement();
        out.writeCharacters("\n");
    }
}