/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classifier;

/**
 *
 * @author sounakbanerjee
 */
public class ReutersTags {
    static String RCV_FILE_START = "<newsitem>";
    static String RCV_INFO = "<newsitem";
    static String RCV_DOCID = "itemid=";
    static String RCV_TITLE_START = "<title>";
    static String RCV_TITLE_END = "</title>";
    static String RCV_HEADLINE_START = "<headline>";
    static String RCV_HEADLINE_END = "</headline>";
    static String RCV_BODY_START = "<text>";
    static String RCV_PARA_BEGIN = "<p>";
    static String RCV_PARA_END = "</p>";
    static String RCV_BODY_END = "</text>";
    static String RCV_CODES_COUNTRY = "<codes class=\"bip:countries:1.0\">";
    static String RCV_CODES_TOPIC = "<codes class=\"bip:topics:1.0\">";
    static String RCV_CODES_INDUSTRY = "<codes class=\"bip:industries:1.0\">";
    static String RCV_CODES = "<code code=";
    static String RCV_CODES_END = "</codes>";
    static String RCV_FILE_END = "<newsitem>";
}
