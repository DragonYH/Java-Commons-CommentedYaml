package cc.commons.commentedyaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

/**
 * 支持注释的Yaml文件配置管理器
 * 
 * @author 聪聪
 *
 */
public class CommentedYamlConfig extends CommentedSection{

    public static class ErrorLog{

        private Logger mLogger=Logger.getGlobal();

        public void severe(String pErrorMsg){
            this.mLogger.log(Level.SEVERE,pErrorMsg);
        }

        public void severe(Throwable pExp){
            this.mLogger.log(Level.SEVERE,pExp.getLocalizedMessage(),pExp);
        }

        public void severe(String pErrorMsg,Throwable pExp){
            this.mLogger.log(Level.SEVERE,pErrorMsg,pExp);
        }

    }

    protected static final String BLANK_CONFIG="{}\n";
    /** 错误日记记录器 */
    protected static ErrorLog mErrorLog=new ErrorLog();
    /** 配置管理器配置 */
    protected CommentedOptions mOptions;
    /** Yaml加载器,非线程安全 */
    protected final Yaml mYaml;
    /** Yaml Dump选项 */
    protected final DumperOptions mDumpOptions;
    /** 仅用于加载,非线程安全 */
    protected final Representer mConfigRepresenter;

    /**
     * 设置错误日志记录器
     * <p>
     * 自定义日志器,只要自定义以下方法即可<br>
     * {@link ErrorLog#severe(String, Throwable)}
     * </p>
     * 
     * @param pLogger
     *            日志记录器
     */
    public static void setLogger(ErrorLog pLogger){
        if(pLogger!=null){
            CommentedYamlConfig.mErrorLog=pLogger;
        }
    }

    /**
     * 获取日志记录器
     * 
     * @return 日志记录器
     */
    public static ErrorLog getLogger(){
        return CommentedYamlConfig.mErrorLog;
    }

    /**
     * 载入配置文件
     * 
     * @param pFile
     *            要载入的配置文件
     * @return 载入的配置管理器
     */
    public static CommentedYamlConfig loadFromFileS(File pFile){
        return CommentedYamlConfig.loadFromFileS(pFile,true);
    }

    /**
     * 载入配置文件
     * 
     * @param pFile
     *            要载入的配置文件
     * @param pEnableComment
     *            是否解析配置文件注释
     * @return 载入的配置管理器
     */
    public static CommentedYamlConfig loadFromFileS(File pFile,boolean pEnableComment){
        CommentedYamlConfig tConfig=new CommentedYamlConfig();
        tConfig.options().enabelComment(pEnableComment);
        tConfig.loadFromFile(pFile);
        return tConfig;
    }

    /**
     * 使用指定的消息前缀实例化配置管理器
     */
    public CommentedYamlConfig(){
        this.mDumpOptions=new DumperOptions();
        this.mDumpOptions.setIndent(2);
        this.mDumpOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.mDumpOptions.setAllowUnicode(true);
        if(!this.mDumpOptions.isAllowUnicode()){
            Class<DumperOptions> clazz=DumperOptions.class;
            try{
                Field field=clazz.getDeclaredField("allowUnicode");
                field.setAccessible(true);
                field.setBoolean(mDumpOptions,true);
            }catch(Exception exp){
                this.log("错误,无法设置文件存储为unicode编码",exp);
            }
        }
        this.mConfigRepresenter=new CommentedRepresenter();
        this.mConfigRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.mYaml=new Yaml(new Constructor(),this.mConfigRepresenter,mDumpOptions);
    }

    /**
     * 在发生异常时用于输出调试信息,或输出警告信息到控制台
     * 
     * @param pMsg
     *            错误的提示消息
     * @param pExp
     *            要抛出的异常
     */
    private void log(String pMsg,Throwable pExp){
        CommentedYamlConfig.mErrorLog.severe(pMsg,pExp);
    }

    /**
     * 将从字符串生成的配置节点数据格式化复制到本实例中
     * 
     * @param input
     *            格式化后的数据
     * @param section
     *            本实例,用于递归
     */
    protected void convertMapsToSections(Map<?,?> input,CommentedSection section){
        if(input==null)
            return;
        for(Map.Entry<?,?> entry : input.entrySet()){
            String key=entry.getKey().toString();
            Object value=entry.getValue();
            if((value instanceof Map))
                convertMapsToSections((Map<?,?>)value,section.createSection(key));
            else section.set(key,value);
        }
    }

    /**
     * 保存当前配置内存中的数据为字符串
     */
    public String saveToString(){
        Map<String,Object> tValues=this.getValues(false);
        Representer tRepresenter=new CommentedRepresenter();
        tRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        String tDumpValue=new Yaml(new Constructor(),tRepresenter,mDumpOptions).dump(tValues);
        if(tDumpValue.equals(BLANK_CONFIG))
            return "";

        if(this.options().isEnableComment()){
            return Composer.putCommentToString(this,tDumpValue);
        }else return tDumpValue;
    }

    /**
     * 保存指定的数据到指定的文件,如果文件不存在将会自动创建
     * <p>
     * 保存数据过程中的任何错误都会被记录到控制台然后忽视
     * </p>
     * 
     * @param pFile
     *            指定的文件
     * @return 是否保存成功
     */
    public boolean saveToFile(File pFile){
        FileOutputStream tOutput=null;
        try{
            byte[] tContent=this.saveToString().getBytes("UTF-8");
            synchronized(this){
                this.tryCreateFile(pFile);
                tOutput=new FileOutputStream(pFile,false);
                tOutput.write(tContent);
            }
        }catch(FileNotFoundException ex){
            this.log("未找到文件["+pFile+"]",ex);
            return false;
        }catch(IOException ex){
            this.log("无法保存文件["+pFile+"]",ex);
            return false;
        }finally{
            if(tOutput!=null)
                try{
                    tOutput.close();
                }catch(IOException exp){
                }
        }
        return true;
    }

    /**
     * 从字符串中载入配置
     */
    public void loadFromString(String pContents) throws YAMLException{
        Map<?,?> input=null;
        try{
            input=(Map<?,?>)this.mYaml.load(pContents);
        }catch(YAMLException e){
            throw e;
        }catch(ClassCastException e){
            throw new YAMLException("配置文件顶级节点不是Map");
        }
        this.clear();
        this.convertMapsToSections(input,this);
        if(this.options().isEnableComment()){
            Composer.collectCommentFromString(this,pContents);
        }
    }

    /**
     * 从给定的文件路径中载入数据
     * <p>
     * 如果文件不存在将会自动创建<br />
     * 载入配置文件过程中的任何错误都会被记录到控制台然后忽视<br />
     * 如果载入失败,配置管理器内容将不变<br />
     * 编码默认使用 UTF-8
     * <p>
     *
     * @return 是否载入成功
     * @param pFilename
     *            输入的文件路径
     * @throws NullPointerException
     *             如果文件名为空
     */
    public boolean loadFromFile(String pFilename){
        return this.loadFromFile(new File(pFilename));
    }

    /**
     * 从给定的文件中载入数据
     * <p>
     * 如果文件不存在将会自动创建<br />
     * 载入配置文件过程中的任何错误都会被记录到控制台然后忽视<br />
     * 如果载入失败,配置管理器内容将不变<br />
     * 编码默认使用 UTF-8
     * <p>
     *
     * @param pFile
     *            输入文件
     * @return 是否成功加载
     * @throws IllegalArgumentException
     *             如果文件为空
     */
    public boolean loadFromFile(File pFile){
        FileInputStream tInput=null;
        try{
            byte[] tContents=new byte[(int)pFile.length()];
            if(!pFile.isFile())
                this.tryCreateFile(pFile);
            tInput=new FileInputStream(pFile);
            tInput.read(tContents);
            this.loadFromString(new String(tContents,"UTF-8"));
        }catch(FileNotFoundException ex){
            this.log("无法找到文件["+pFile+"]",ex);
            return false;
        }catch(IOException ex){
            this.log("无法加载文件["+pFile+"]",ex);
            return false;
        }catch(YAMLException ex){
            this.log("无法加载文件["+pFile+"],配置文件格式错误",ex);
            return false;
        }finally{
            if(tInput!=null)
                try{
                    tInput.close();
                }catch(IOException exp){
                }
        }
        return true;
    }

    /**
     * 从给定的流中载入数据,流不会自动关闭
     * <p>
     * 如果文件不存在将会自动创建<br />
     * 载入配置文件过程中的任何错误都会被记录到控制台然后忽视<br />
     * 如果载入失败,配置管理器内容将不变<br />
     * 编码默认使用 UTF-8
     * <p>
     *
     * @param stream
     *            输入的数据流
     * @return 是否载入成功
     * @throws IllegalArgumentException
     *             如果输入流为空
     */
    public boolean loadFromStream(InputStream stream){
        try{
            byte[] contents=new byte[stream.available()];
            this.loadFromString(new String(contents,"UTF-8"));
        }catch(IOException ex){
            this.log("无法从输入流加载配置",ex);
            return false;
        }catch(YAMLException ex){
            this.log("无法从输入流加载配置,配置文件格式错误",ex);
            return false;
        }
        return true;
    }

    /**
     * 创建一个新文件<br/>
     * 如果文件已经存在,将什么都不干
     * 
     * @param pFile
     *            文件
     * @throws IOException
     *             创建文件时发生错误
     */
    protected void tryCreateFile(File pFile) throws IOException{
        if(pFile.exists()&&pFile.isFile())
            return;
        pFile=pFile.getAbsoluteFile();
        if(!pFile.getParentFile().isDirectory()){
            pFile.getParentFile().mkdirs();
        }
        pFile.createNewFile();
    }

    /**
     * 设置Yaml文件单极的空格缩进个数
     * 
     * @param pIndent
     *            缩进
     */
    public void setIndent(int pIndent){
        this.mDumpOptions.setIndent(pIndent);
    }

    /**
     * 获取Yaml文件单极的空格缩进个数
     */
    public int getIndent(){
        return this.mDumpOptions.getIndent();
    }

    public CommentedOptions options(){
        synchronized(this){
            if(mOptions==null){
                mOptions=new CommentedOptions();
            }
        }
        return mOptions;
    }

}
