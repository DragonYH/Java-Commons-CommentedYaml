package cc.commons.commentedyaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

enum Mode{
    /** 将配置管理器中的注释导入到dump出来的字符串中 */
    Inject,
    /** 从字符串中读取出注释 */
    Export
}

public class Composer{

    /** Yaml节点边界符号 */
    private final static HashSet<Character> mWrapChars;
    /** Yaml源格式字符标识 */
    private final static HashSet<Character> mRawChars;
    /** Yaml单个权重的缩进量,由处理文本时自动确定 */
    private int mIndent=0;
    /**
     * 请勿直接调用该变量<br>
     * 使用{@link Composer#alreadyHandleLine()}来更改已经处理的当前行<br>
     * 使用{@link Composer#getNextUnhandleLine()}来获取下一行未处理的文本<br>
     * 使用{@link Composer#haveUnhandleLine()}来判断是否已经处理完所有行文本<br>
     */
    @Deprecated
    private int mLineIndex=-1;
    /** 原文本的行 */
    private String[] mSourceLines;
    /** 原文本的行,内容可能会在程序处理过程中发生变化 */
    private String[] mLines;
    /** 当前绑定的配置管理器 */
    private CommentedYamlConfig mConfig;
    /** 当前与注释合并的文本 */
    private final ArrayList<String> mContent=new ArrayList<>();
    /** 当前从文本中提取出且未保存到配置管理器中的注释 */
    private ArrayList<String> mComment=new ArrayList<>();
    /** 当前最后一条缓存的注释所在的行数 */
    private int mCommentLineIndex=-1;
    /** 注释操作模式 */
    private Mode mMode;

    static{
        mWrapChars=new HashSet<>();
        mWrapChars.add('\'');
        mWrapChars.add('"');
        mRawChars=new HashSet<>();
        mRawChars.add('<');
        mRawChars.add('|');
    }

    private Composer(CommentedYamlConfig pConfig,String pContent,Mode pMode){
        this.mMode=pMode;
        this.mConfig=pConfig;

        this.mSourceLines=pContent.split("[\\r]?\n");
        this.mLines=Arrays.copyOf(this.mSourceLines,this.mSourceLines.length);
    }

    /**
     * 从给予的文本中搜索节点的注释,并导入到配置管理器中
     * <p>
     * 如果导入出错,导入将终止,但是已经导入的注释将会保持
     * </p>
     * 
     * @param pConfig
     *            配置管理器
     * @param pContent
     *            文本
     * @return 是否导入成功
     */
    public static boolean collectCommentFromString(CommentedYamlConfig pConfig,String pContent){
        return Composer.convert(new Composer(pConfig,pContent,Mode.Export));
    }

    /**
     * 将配置管理器中的注释输出并嵌入到文本中
     * <p>
     * 如果合并出错,合并将终止,函数将返回未合并的初始文本
     * </p>
     * 
     * @param pConfig
     *            配置管理器
     * @param pContent
     *            文本
     * @return 合并后的文本
     */
    public static String putCommentToString(CommentedYamlConfig pConfig,String pContent){
        Composer tComposer=new Composer(pConfig,pContent,Mode.Inject);
        if(Composer.convert(tComposer)){
            StringBuilder builder=new StringBuilder();
            for(String sStr : tComposer.mContent){
                builder.append(sStr);
                builder.append(System.getProperty("line.separator","\r\n"));
            }
            return builder.toString();
        }
        return pContent;
    }

    /**
     * 导入或导出注释
     * 
     * @param pConfig
     *            配置管理器
     * @param pContent
     *            字符内容
     * @param pMode
     *            模式(导入/导出)
     * @return 是否无错误发生
     */
    private static boolean convert(Composer pComposer){
        YamlNode rootNode=new YamlNode();
        rootNode.mName="";
        rootNode.setParent(rootNode);
        try{
            while(pComposer.haveUnhandleLine()){
                pComposer.convertNode(rootNode,-1);
            }
        }catch(IllegalStateException exp){
            CommentedYamlConfig.getLogger().severe(exp.getMessage());
            return false;
        }catch(Throwable exp){
            CommentedYamlConfig.getLogger().severe("导入导出配置文件注释时发生了错误",exp);
            return false;
        }
        return true;
    }

    /**
     * 测试用函数
     */
    @Deprecated
    private YamlNode getMapValue(){
        YamlNode rootNode=new YamlNode();
        rootNode.mName="";
        while(this.mLineIndex<this.mLines.length){
            this.convertNode(rootNode,-1);
        }
        return rootNode;
    }

    /**
     * 转换当前行为数组
     * 
     * @param pWeight
     *            当前行权重
     * @param pListDeep
     *            当前行数组深度
     * @param pLine
     *            当前行内容
     * @return 转换成的数组或字符串或其他嵌套
     */
    private Object convertList(int pWeight,int pListDeep,String pLine){
        ArrayList<Object> listValue=new ArrayList<>();
        while(true){
            if(!this.isCloseLine(pLine)){
                String partLine=null;
                this.alreadyHandleLine();
                if(!this.haveUnhandleLine()){
                    this.log("行未闭合");
                    return pLine+pLine.charAt(0);
                }
                partLine=this.getNextUnhandleLine();
                pLine+=partLine.trim();
            }
            LineObject tLine=this.getLineType(pLine);
            switch(tLine.mLineType){
                case Comment:
                    this.log("#字符必须包裹在单引号之间,或List的值后不能添加注释");
                    return "''";
                case List:
                    listValue.add(this.convertList(pWeight,pListDeep+1,tLine.mValue));
                    break;
                case Node_Empty:
                    this.setNextUnhandleLine(this.getBlank(this.mIndent*(pWeight+pListDeep))+tLine.mName+":");
                    return this.convertNode(null,pWeight+pListDeep-1);
                case Node_Valued:
                    this.setNextUnhandleLine(this.getBlank(this.mIndent*(pWeight+pListDeep))+tLine.mName+": "+tLine.mValue);
                    return this.convertNode(null,pWeight+pListDeep-1);
                case String:
                    this.alreadyHandleLine();
                    if(this.isCloseLine(tLine.mValue))
                        return tLine.mValue;
                    else{
                        this.setNextUnhandleLine(this.getBlank(this.mIndent*(pWeight+pListDeep))+tLine.mValue+(this.getNextUnhandleLine().trim()));
                        break;
                    }
            }
            while(true){
                if(!this.haveUnhandleLine())
                    return listValue;
                pLine=this.getNextUnhandleLine();
                int tWeight=-1;
                while((tWeight=this.getWeight(pLine))==-1){
                    this.alreadyHandleLine();
                    this.addCacheComment(pLine);
                    if(!this.haveUnhandleLine())
                        return listValue;
                    pLine=this.getNextUnhandleLine();
                }
                if(tWeight==pWeight+pListDeep){ // 同级别的行,在本函数中处理
                    break;
                }else if(tWeight>pWeight+pListDeep){
                    this.log("缩进错误");
                    this.alreadyHandleLine();
                    continue;
                }else{ // 高级别行,在上个函数中处理
                    return listValue;
                }
            }
        }
    }

    private Object convertNode(YamlNode pParent,int pParentWeight){
        YamlNode lastChild=null;
        String nowLine;
        ArrayList<String> fullPath;
        while(this.haveUnhandleLine()){
            nowLine=this.getNextUnhandleLine();
            int tWeight=-1;
            while((tWeight=this.getWeight(nowLine))==-1){
                this.alreadyHandleLine();
                this.addCacheComment(nowLine);
                if(!this.haveUnhandleLine())
                    return lastChild;
                nowLine=this.getNextUnhandleLine();
            }
            if(tWeight==pParentWeight){ // 与父节点同级
                return lastChild;
            }
            if(tWeight==pParentWeight+1){ // 直接子节点
                YamlNode nowNode=new YamlNode();
                LineObject tLine=this.getLineType(nowLine);
                nowNode.mName=tLine.mName;
                switch(tLine.mLineType){ // 此处不可能是注释节点
                    case List:
                        if(lastChild==null){
                            this.log("错误的缩进");
                            this.alreadyHandleLine();
                            continue;
                        }
                        this.convertList(pParentWeight+1,1,tLine.mValue);
                        continue;
                    case Comment:
                        // 不可能为注释
                        break;
                    case Node_Valued:
                        nowNode.setParent(pParent);
                        // 由于alreadyHandleLine会设置文本到List,所以先设置注释,再调用alreadyHandleLine();
                        if(this.mMode==Mode.Export){
                            this.alreadyHandleLine();
                            if(!this.mComment.isEmpty()&&(fullPath=nowNode.getPathList())!=null){
                                this.mConfig.setComments(fullPath,this.mComment);
                                this.mComment.clear();
                            }
                        }else{
                            if((fullPath=nowNode.getPathList())!=null){
                                this.addCommentToContent(pParentWeight+1,this.mConfig.getComments(fullPath));
                            }
                            this.alreadyHandleLine();
                        }

                        // 读取剩余部分的值
                        if(!this.isCloseLine(tLine.mValue)){
                            char tWarpChar=tLine.mValue.trim().charAt(0);
                            Character tCloseMark=null;
                            String partLine=null;
                            do{
                                if(!this.haveUnhandleLine()){
                                    this.log("行未闭合");
                                    return nowNode;
                                }
                                partLine=this.getNextUnhandleLine();
                                tLine.mValue+=partLine.trim();
                                this.alreadyHandleLine();
                                tCloseMark=this.getCloseMark(partLine);
                            }while(tCloseMark==null||!tCloseMark.equals(tWarpChar));
                        }

                        // 是不是raw格式
                        if(tLine.mValue.length()==1&&Composer.mRawChars.contains(tLine.mValue.charAt(0))){
                            tLine.mValue=this.readRawContent(pParentWeight+1);
                        }
                        if(tLine.mValue.startsWith("!!")&&tLine.mValue.length()>2){
                            lastChild=nowNode;
                        }else return nowNode;
                    case Node_Empty:
                        nowNode.setParent(pParent);

                        if(this.mMode==Mode.Export){
                            this.alreadyHandleLine(); //先调用此方法移动已处理行数,用于添加注释
                            if(tLine.mValue!=null){ //带注释的无值节点
                                this.addCacheComment(tLine.mValue);
                            }
                            if(!this.mComment.isEmpty()&&(fullPath=nowNode.getPathList())!=null){
                                this.mConfig.setComments(fullPath,this.mComment);
                                this.mComment.clear();
                            }
                        }else{
                            if((fullPath=nowNode.getPathList())!=null){
                                this.addCommentToContent(pParentWeight+1,this.mConfig.getComments(fullPath));
                            }
                            this.alreadyHandleLine(); // 先设置注释,再调用此方法设置内容
                        }

                        lastChild=nowNode;
                        break;
                    case String:
                        this.alreadyHandleLine();
                        if(this.isCloseLine(tLine.mValue)){
                            this.log("此处不应该有"+nowLine+",是否缺少冒号?");
                            continue;
                        }else{
                            if(!this.haveUnhandleLine()){
                                this.log("行未闭合");
                                continue;
                            }
                            this.setNextUnhandleLine(this.getBlank(this.mIndent*(pParentWeight+1))+tLine.mValue+(this.getNextUnhandleLine().trim()));
                            continue;
                        }
                }
            }else if(tWeight==pParentWeight+2){ // 孙子节点
                if(lastChild==null){
                    this.log("错误的缩进");
                    this.alreadyHandleLine();
                    continue;
                }
                this.convertNode(lastChild,pParentWeight+1);
            }else if(tWeight<pParentWeight){ // 上级节点
                if(lastChild==null){
                    this.log("错误的缩进");
                    this.alreadyHandleLine();
                    continue;
                }
                return lastChild;
            }else{ // 下下...级节点,缩进过多
                this.log("错误的缩进");
                this.alreadyHandleLine();
                continue;
            }
        }
        return null;
    }

    /**
     * 读取从当前行开始的raw内容
     * 
     * @param pWeight
     *            父节点的权重
     * @return 读取的内容
     */
    private String readRawContent(int pWeight){
        int minBlackCount=pWeight*this.mIndent,currentBlackCount=-1;
        StringBuilder rawContent=new StringBuilder();
        while(this.haveUnhandleLine()){
            String tLineContent=this.getNextUnhandleLine();
            if(this.isBlank(tLineContent)){//空白行忽略
                this.alreadyHandleLine();
                continue;
            }
            int tIndex;
            for(tIndex=0;tIndex<tLineContent.length();tIndex++){
                if(tLineContent.charAt(tIndex)!=' ')
                    break;
            }
            if(tIndex<=minBlackCount) // raw文本结束
                break;

            if(currentBlackCount==-1){
                currentBlackCount=tIndex;
            }

            if(rawContent.length()!=0)
                rawContent.append("\n");
            try{
                rawContent.append(tLineContent.substring(currentBlackCount));
            }catch(IndexOutOfBoundsException exp){
                // 此处配置已经出现了错误
                rawContent.append(tLineContent.substring(tIndex));
            }
            this.alreadyHandleLine();
        }
        return rawContent.toString();
    }

    /**
     * 是否还有未处理的行
     */
    private boolean haveUnhandleLine(){
        return this.mLineIndex+1<this.mLines.length;
    }

    /**
     * 获取下一行未处理的行文本
     * <p>
     * 如果没有下一行了,将返回null
     * </p>
     * 
     * @return 文本或null
     */
    private String getNextUnhandleLine(){
        if(!this.haveUnhandleLine())
            return null;
        return this.mLines[this.mLineIndex+1];
    }

    /**
     * 设置下一行未处理文本的内容
     * 
     * @param pNewLine
     *            新的文本
     * @return 是否设置成功
     */
    private boolean setNextUnhandleLine(String pNewLine){
        if(!this.haveUnhandleLine())
            return false;
        this.mLines[this.mLineIndex+1]=pNewLine;
        return true;
    }

    /**
     * 指示已处理行号加1,并将已经处理行的源文本放入{@link Composer#mContent}中
     */
    private void alreadyHandleLine(){
        this.mLineIndex++;
        if(this.mContent!=null){
            this.mContent.add(this.mSourceLines[this.mLineIndex]);
        }
    }

    /**
     * 根据指定的数量生成指定长度的空白字符串
     */
    private String getBlank(int pCount){
        if(pCount<=0)
            return "";
        StringBuilder sb=new StringBuilder();
        while(pCount-->0){
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * 添加注释内容{@link Composer#mContent}中
     * 
     * @param pWeight
     *            注释行的权重
     * @param pComments
     *            注释文本
     */
    private void addCommentToContent(int pWeight,ArrayList<String> pComments){
        if(pComments==null||pComments.isEmpty())
            return;
        String blackPrefix=this.getBlank(pWeight*this.mIndent)+"# ";
        for(String commentLine : pComments)
            this.mContent.add(blackPrefix+commentLine);
    }

    /**
     * 添加注释缓存到{@link Composer#mComment}中,并设置{@link Composer#mLineIndex}为该注释的所在的行数<br>
     * 当识别到Node时,再将注释缓存设置到配置管理器中
     * <p>
     * 注意此方法只在{@link Mode#Export}模式下调用
     * </p>
     * 
     * @param pLine
     *            行
     * @param pReset
     *            是否清空已有注释
     */
    private void addCacheComment(String pLine){
        if(this.mCommentLineIndex+1!=this.mLineIndex){
            this.mComment.clear();
        }

        int startPos=0;
        char tc;
        for(;startPos<pLine.length();startPos++){
            tc=pLine.charAt(startPos);
            if(tc!=' '){
                if(tc=='#'){
                    startPos++;
                    if(pLine.length()>startPos&&pLine.charAt(startPos)==' ')
                        startPos++;
                }
                break;
            }
        }

        if(startPos>=pLine.length()){ //全空格
            this.mComment.add(pLine);
        }else{
            this.mComment.add(pLine.substring(startPos));
        }
        this.mCommentLineIndex=this.mLineIndex;
    }

    /**
     * 获取行的权重
     * <p>
     * 如果返回-1,说明该行是注释行
     * </p>
     * 
     * @param pLine
     *            行
     * @return 权重
     */
    private int getWeight(String pLine){
        if(this.isBlank(pLine))
            return -1;
        char[] tLineChars=pLine.toCharArray();
        int i=0,weight=-1;
        for(;i<tLineChars.length;i++)
            if(tLineChars[i]!=' ')
                break;
        if(i==0)
            weight=0;
        else{
            if(this.mIndent==0)
                this.mIndent=i;
            weight=i/this.mIndent;
        }
        if(tLineChars[i]=='#')
            return -1;
        else return weight;
    }

    private LineObject getLineType(String pLine){
        pLine=pLine.trim();

        LineObject tLine=new LineObject();
        if(this.isBlank(pLine)){
            tLine.mLineType=LineType.Comment;
            tLine.mValue=pLine;
            return tLine;
        }

        if(pLine.charAt(0)=='#'){
            tLine.mLineType=LineType.Comment;
            if(pLine.length()>=2&&pLine.charAt(1)==' ')
                tLine.mValue=pLine.substring(2);
            else tLine.mValue=pLine.substring(1);
            return tLine;
        }else if(pLine.length()>=2&&pLine.charAt(0)=='-'&&pLine.charAt(1)==' '){
            tLine.mLineType=LineType.List;

            int tPos=2;
            for(;tPos<pLine.length();tPos++)
                if(pLine.charAt(tPos)!=' ')
                    break;
            if(tPos>=pLine.length()){
                tLine.mValue=new String();
            }else{
                tLine.mValue=pLine.substring(tPos);
            }

            return tLine;
        }else{
            char[] arrs=pLine.toCharArray();
            int i=1;
            char tWarpChar=arrs[0];
            boolean warp=Composer.mWrapChars.contains(tWarpChar);
            boolean tNameWarp=warp;
            while(i<arrs.length){
                char c=arrs[i++];
                if(warp){
                    if(c==tWarpChar){
                        if(i>=arrs.length)
                            break; // 没字符了,作为字符串处理
                        if(tWarpChar=='\''){
                            if(arrs[i]!=tWarpChar){
                                warp=false; // '后面不是单引号,warp到尾部
                                if(arrs[i]!=':'){
                                    this.log("第"+this.mLineIndex+"可能配置错误,缺少冒号?");
                                }
                            }else i++; // 如果是单引号,跳过这个字符的检查
                        }else{ // 检查是否进行\转义
                            if(i>1&&arrs[i-1]=='\\')
                                ; // \转义了
                            else warp=false;
                        }
                    }
                }else{
                    if(c==':'){
                        if(i>=arrs.length){ // 不带值的节点
                            tLine.mLineType=LineType.Node_Empty;
                            int start=0,end=arrs.length-1;
                            if(tNameWarp){
                                start++;
                                end--;
                            }

                            if(start>=end){// 空标签节点
                                // this.log("使用了空标签");
                            }else{
                                tLine.mName=new String(arrs,start,end-start);
                            }

                            return tLine;
                        }
                        if(arrs[i]==' '){ // 带值的节点,后面肯定有值,因为已经trim过了
                            tLine.mLineType=LineType.Node_Valued;
                            int start=0,end=i-1;
                            if(tNameWarp){
                                start++;
                                end--;
                            }

                            if(start>=end){// 空标签节点
                                // this.log("使用了空标签");
                            }else{
                                tLine.mName=new String(arrs,start,end-start);
                            }

                            tLine.mValue=new String(arrs,i+1,arrs.length-i-1);
                            String tValue=tLine.mValue.trim();
                            if(tValue.startsWith("#")){
                                tLine.mLineType=LineType.Node_Empty;
                            }
                            return tLine;
                        }
                    }
                }
            }
            tLine.mLineType=LineType.String;
            tLine.mValue=new String(arrs);
            return tLine;
        }
    }

    private void log(String pMsg){
        String showMsg="第"+(this.mLineIndex+2)+"行配置错误,"+pMsg+",无法导入注释\n请提供你的配置文件给作者以供分析格式";
        throw new IllegalStateException(showMsg);
    }

    private boolean isCloseLine(String pLine){
        if(this.isBlank(pLine))
            return true;
        pLine=pLine.trim();
        char tFirstChar=pLine.charAt(0),tLastChar=pLine.charAt(pLine.length()-1);
        if(Composer.mWrapChars.contains(tFirstChar)){
            if(pLine.length()==1)
                return false;
            if(pLine.length()==2)
                return true;
            if(tFirstChar=='\''){
                // 在内部的单引号一定是成对出现的
                pLine=pLine.substring(1,pLine.length()).replace("''","");
                return pLine.charAt(pLine.length()-1)=='\'';
            }else{
                if(pLine.charAt(pLine.length()-2)!='\\'&&tLastChar==tFirstChar){ //是一对边界符,且最后一个字符没有被转义
                    return true;
                }else{
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取关闭标识字符,如果不存在,返回null
     */
    private Character getCloseMark(String pLine){
        if(this.isBlank(pLine))
            return null;
        Character tc=pLine.charAt(pLine.length()-1);
        if(Composer.mWrapChars.contains(tc)){
            return tc;
        }else return null;
    }

    private boolean isBlank(String pStr){
        if(pStr==null||pStr.isEmpty())
            return true;

        char[] tContent=pStr.toCharArray();
        for(int i=0;i<tContent.length;i++){
            if(!Character.isWhitespace(tContent[i]))
                return false;
        }
        return true;
    }

}
