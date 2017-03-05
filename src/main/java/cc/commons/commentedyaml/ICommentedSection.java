package cc.commons.commentedyaml;

import java.util.ArrayList;

public interface ICommentedSection{

    public void addComments(String pPath,String...pComments);

    /**
     * 添加默认注释
     * <p>
     * 如果注释已经存在,将忽略
     * </p>
     * 
     * @param pComments
     *            评论
     */
    public void addDefaultComments(String pPath,String...pComments);

    /**
     * 设置注释
     * <p>
     * 请勿设置null来清空注释<br />
     * 如果要清空注释,请使用{@link CommentedValue#clearComments()}
     * </p>
     * /
     * 
     * @param pComments
     *            注释
     */
    public void setComments(String pPath,String...pComments);

    /**
     * 获取指定路径下的注释的拷贝
     * 
     * @return 非null
     */
    public ArrayList<String> getComments(String pPath);

    /**
     * 节点是否有注释
     */
    public boolean hasComments(String pPath);

    /**
     * 清空指定路径下的注释
     * 
     * @return 清理掉的注释,非null
     */
    public ArrayList<String> clearComments(String pPath);

}
