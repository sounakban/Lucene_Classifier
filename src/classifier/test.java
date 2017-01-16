/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classifier;


import static common.CommonVariables.FIELD_BOW;
import static common.CommonVariables.FIELD_ID;

/**
 *
 * @author sounakbanerjee
 */
public class test {
    
    static<T> void println(T arg) { System.out.println(arg); }
    
    public static void main(String[] args) {
        
        // ###########Find variable types and content#########
        println("\n"+"###########Find variable types and content#########");
        println("Field_ID : type : "+FIELD_ID.getClass().getName()+"     Content : "+FIELD_ID);
        println("Field_BOW : type : "+FIELD_BOW.getClass().getName()+"     Content : "+FIELD_BOW);
        
        // ###########String Builder Experiments#########
        println("\n"+"###########String Builder Experiments#########");
        StringBuilder sb = new StringBuilder("");
        sb.append("test");
        println("Test to see StrBld: "+sb);
        sb.deleteCharAt(sb.length()-1);
        println("After deleteCharAt: "+sb);
    }
}
