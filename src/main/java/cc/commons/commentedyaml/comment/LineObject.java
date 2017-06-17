package cc.commons.commentedyaml.comment;

public class LineObject{

    public final String mContent;
    public LineType mType;
    public String mName;
    public String mValue;

    protected LineObject(String pContent){
        this.mContent=pContent;
    }

}
