package cc.commons.commentedyaml;

import org.yaml.snakeyaml.DumperOptions;

public class CommentedOptions extends DumperOptions{

    private char mPathSeparator='.';
    private boolean mSaveComment=true;

    protected CommentedOptions(){}

    public char pathSeparator(){
        return this.mPathSeparator;
    }

    public void pathSeparator(char value){
        this.mPathSeparator=value;
    }

    public boolean isEnableComment(){
        return this.mSaveComment;
    }

    /**
     * 启用或停用配置文件的注释
     * 
     * @return 之前的注释启用状态
     */
    public boolean enabelComment(boolean pEnable){
        boolean oldStatus=this.mSaveComment;
        this.mSaveComment=pEnable;
        return oldStatus;
    }

}
