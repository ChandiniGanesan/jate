package uk.ac.shef.dcs.jate.v2.feature;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zqz on 21/09/2015.
 */
public class FrequencyCtxBased extends AbstractFeature {
    //context and total term freq in context
    private Map<Integer, Integer> ctx2TTF = new HashMap<>();
    //context and its contained term with their Freq In Context
    private Map<Integer, Map<String, Integer>> ctx2TFIC = new HashMap<>();

    protected int contextCounter=-1;

    protected FrequencyCtxBased() {
    }

    protected int nextCtxId(){
        contextCounter++;
        return contextCounter;
    }

    public Map<Integer, Integer> getMapCtx2TTF(){
        return ctx2TTF;
    }

    public Map<Integer, Map<String, Integer>> getMapCtx2TFIC(){
        return ctx2TFIC;
    }

    public Map<String, Integer> getTFIC(int docId){
        Map<String, Integer> result = ctx2TFIC.get(docId);
        if(result==null)
            return new HashMap<>();
        return result;
    }

    /**
     * increment the number of occurrences of term by i
     *
     */
    protected void increment(int ctxId, String term, int tf) {
        Map<String, Integer> tfidMap = ctx2TFIC.get(ctxId);
        if(tfidMap==null)
            tfidMap = new HashMap<>();

        Integer f = tfidMap.get(term);
        if(f==null)
            f=0;
        f+=tf;
        tfidMap.put(term, f);
        ctx2TFIC.put(ctxId, tfidMap);
    }

    protected void increment(int ctxId, int freq){
        Integer f = ctx2TTF.get(ctxId);
        if(f==null)
            f=0;
        f+=freq;
        ctx2TTF.put(ctxId, f);
    }
}
