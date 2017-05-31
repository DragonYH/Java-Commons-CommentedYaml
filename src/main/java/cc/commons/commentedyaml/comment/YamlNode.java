package cc.commons.commentedyaml.comment;

import java.util.ArrayList;

public class YamlNode{

    public static YamlNode empty;

    private ArrayList<String> mComment=new ArrayList<>();
    /** 仅当节点为根节点时,此值才会Empty */
    public String mName;
    private YamlNode mParent;

    protected void setComment(ArrayList<String> pComment){
        if(pComment==null||pComment.isEmpty())
            return;
        this.mComment.clear();
        this.mComment.addAll(pComment);
    }

    /**
     * 获取当前节点到根节点的路径
     * <p>
     * 如果根节点为null,那么返回null
     * </p>
     * 
     * @param pPathSeparator
     *            路径拼接字符
     * @return 拼接的路径或null
     */
    public ArrayList<String> getPathList(/* char pPathSeparator */){
        if(this.mParent==null){
            return null;
        }else if(this.mParent==this){
            return null;
        }else{
            YamlNode tNode=this;
            ArrayList<String> tPath=new ArrayList<>();
            while(tNode.mParent!=null){
                if(tNode==tNode.mParent)
                    return tPath;

                tPath.add(0,tNode.mName);
                tNode=tNode.mParent;
            }
            return null;
        }
    }

    public void setParent(YamlNode pNode){
        this.mParent=pNode;
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder();
        if(this.mName==null||this.mName.isEmpty())
            this.mName="";
        return sb.append("\"").append(mName).append("\"").append(":").toString();
    }

    private static String getBlank(int pCount){
        if(pCount<=0)
            return "";
        StringBuilder sb=new StringBuilder();
        while(pCount-->0){
            sb.append(' ');
        }
        return sb.toString();
    }

}
