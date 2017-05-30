package cc.commons.commentedyaml;

import org.yaml.snakeyaml.error.YAMLException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by xjboss on 2017/5/30.
 */
public class SerializableYamlUtils {
    /**
     * 反序列化指定的数据到指定的类型,如果类型不存在你就会狗带
     * <p>
     * 保存数据过程中的任何错误都会被记录到控制台然后忽视
     * </p>
     *
     * @param pSection
     *            指定的节点
     * @param pObj
     *            指定的对象
     * @param pClass
     *            指定的类型
     * @return T
     */
    public static <T extends SerializableYamlObject> T saveToObject(CommentedSection pSection, T pObj, Class<T> pClass) throws YAMLException {
        return saveToObject(pSection.values(), pObj, pClass);
    }
    private static  <T extends SerializableYamlObject> T saveToObject(Map<String,CommentedValue> pInput, T pObj, Class<T> pClass) throws YAMLException {
        try {
            T obj=pClass.newInstance();
            Field[] fields = pClass.getDeclaredFields();
            HashSet<Integer> founded = new HashSet<>();
            HashSet<Integer> all = new HashSet<>();
            for (Map.Entry<String, CommentedValue> me:pInput.entrySet()) {
                String k = me.getKey();
                CommentedValue v=me.getValue();
                for (int i = 0; i < fields.length; i++) {
                    if (founded.contains(i)) continue;
                    all.add(i);
                    Field f = fields[i];
                    int mod=f.getModifiers();
                    if(Modifier.isFinal(mod)||Modifier.isStatic(mod)||Modifier.isTransient(mod)){
                        founded.add(i);
                        continue;
                    }
                    f.setAccessible(true);
                    Object fo=f.get(obj);
                    Object vv=v.getValue();
                    ArrayList<String> comments=v.getComments();
                    if (f.getName().equals(k)) {
                        if(comments!=null) obj.comments.put(k,comments);
                        founded.add(i);
                        if(vv instanceof Map&&fo instanceof Map){
                            ((Map)fo).clear();
                            ((Map) fo).putAll(ConvertMapObject(k,fo.getClass(),(Map<String,Object>)vv,pObj));
                            break;
                        }else if(vv instanceof Map&&fo instanceof SerializableYamlObject){
                            fo=saveToObject((Map<String,CommentedValue>)vv,(SerializableYamlObject) fo,(Class<SerializableYamlObject>)fo.getClass());
                        } else if(vv instanceof List &&fo instanceof List){
                            ((List) fo).clear();
                            ((List) fo).addAll(ConvertListObject(fo.getClass(),(List<Object>)vv));
                            break;
                        }else if(vv.getClass()!=fo.getClass()){
                            throw new YAMLException(String.format("节点%s类型不对",k));
                        }
                        f.set(obj,fo);
                        break;
                    }
                }
            }
            if (pObj==null)return obj;
            all.removeAll(founded);
            for(int i:all){
                Field f=fields[i];
                f.setAccessible(true);
                f.set(obj,f.get(pObj));
            }
            return obj;
        }catch (Exception e){
            throw new YAMLException("你这个类有毒");
        }
    }
    private static <T extends SerializableYamlObject,M> Map ConvertMapObject(String pMapName, Class<M> pMapClass, Map<String,Object> pNmap, T pT){
        try {
            Type[] GT = pMapClass.getClass().getGenericInterfaces();
            Type K = GT[0];
            Type V = GT[1];
            Map newMap = (Map)pMapClass.newInstance();
            boolean VMap=V instanceof Map;
            boolean VList=V instanceof List;

            final java.lang.reflect.Constructor Cons;
            if (K instanceof Number) {
                Cons= K.getClass().getConstructor(String.class);
            }else{
                Cons=null;
            }
            for (Map.Entry<String, Object> me:pNmap.entrySet()) {
                final String key=me.getKey();
                String tag=null;
                final Object v=me.getValue();
                final Object vv;
                if(v instanceof CommentedValue){
                    tag = pMapName + "." + key;
                    vv=me.getValue();
                    pT.comments.put(tag, ((CommentedValue) v).getComments());
                }else{
                    vv=v;
                }
                if (VMap) {
                    if(Cons==null) {
                        newMap.put(key, ConvertMapObject(tag,V.getClass(),pNmap,pT));
                    }else{
                        newMap.put(Cons.newInstance(key),ConvertMapObject(tag,V.getClass(),pNmap,pT));
                    }
                }else if(VList){
                    if(Cons==null) {
                        newMap.put(key, ConvertListObject(V.getClass(),(List<Object>)v));
                    }else{
                        newMap.put(Cons.newInstance(key),ConvertListObject(V.getClass(),(List<Object>)v));
                    }
                } else {
                    if(Cons==null) {
                        newMap.put(key, vv);
                    }else{
                        newMap.put(Cons.newInstance(key),vv);
                    }
                }
            }
            return newMap;
        }catch(Exception e){
            try {
                return (Map)pMapClass.newInstance();
            }catch(Exception ee){return null;}
        }
    }
    private static <L> List ConvertListObject(Class<L> pListClass,List<Object> pNewList){
        try{
            Type[] GT = pListClass.getClass().getGenericInterfaces();
            Type V=GT[0];
            List newList=(List)pListClass.newInstance();
            boolean VMap=V instanceof Map;
            boolean VList=V instanceof List;
            for (Object no:pNewList){
                if(VMap){
                    no=ConvertMapObject(null,no.getClass(),(Map<String, Object>) no,null);
                }else if(VList){
                    no=ConvertListObject(no.getClass(),(List<Object>) no);
                }
                newList.add(no);
            }
            return newList;
        }catch (Exception e){
            try{
                return (List)pListClass.newInstance();
            }catch (Exception ee){return null;}
        }
    }
}
