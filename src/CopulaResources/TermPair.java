/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CopulaResources;

import java.io.Serializable;

/**
 *
 * @author sounak
 */


public class TermPair implements Serializable {
    private final String term1;
    private final String term2;
    
    public TermPair(String t1, String t2) {
        this.term1 = t1;
        this.term2 = t2;
    }
    
    
    public String getTerm1() {
        return term1;
    }
    
    
    public String getTerm2() {
        return term2;
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof TermPair) {
            TermPair s = (TermPair)obj;
            return (term1.equals(s.term1) && term2.equals(s.term2)) ||
                    (term2.equals(s.term1) && term1.equals(s.term2));
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int hash1 = (term1+term2).hashCode();
        int hash2 = (term2+term1).hashCode();
        return (hash1>hash2)?hash2:hash1;
    }
}
