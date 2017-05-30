package cc.commons.commentedyaml;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

import org.yaml.snakeyaml.error.YAMLException;

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
    private static  <T> T saveToObject(Map<String,CommentedValue> pInput, T pObj, Class<?> pClass) throws YAMLException {
        try {
            SerializableYamlObject obj=(SerializableYamlObject)pClass.newInstance();
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
                        if(vv instanceof CommentedSection){
                            if(fo instanceof Map) {
                                ((Map) fo).clear();
                                Map mv = ((CommentedSection) vv).values();
                                ((Map) fo).putAll(ConvertMapObject(k, fo.getClass(), (Map<String, Object>) mv, obj));
                            }else if(fo instanceof SerializableYamlObject){
                                fo=saveToObject(((CommentedSection) vv).values(),fo,f.getType());
                            }
                        }else if(vv instanceof Collection &&fo instanceof Collection) {
                            ((Collection) fo).clear();
                            ((Collection) fo).addAll(ConvertCollectionObject(fo.getClass(), (Collection<Object>) vv));
                            break;
                        }else if(vv.getClass()!=fo.getClass()){
                            throw new YAMLException(String.format("节点%s类型不对",k));
                        }else{
                            fo=vv;
                        }
                        f.set(obj,fo);
                        break;
                    }
                }
            }
            if (pObj==null)return (T)obj;
            all.removeAll(founded);
            for(int i:all){
                Field f=fields[i];
                f.setAccessible(true);
                f.set(obj,f.get(pObj));
            }
            return (T)obj;
        }catch (Exception e){
            e.printStackTrace();
            throw new YAMLException("你这个类有毒");
        }
    }
    private static <T,M> Map ConvertMapObject(String pMapName, Class<M> pMapClass, Map<String,Object> pNmap, T pT){
        try {
            Type[] GT = pMapClass.getClass().getGenericInterfaces();
            Type K = GT[0];
            Type V = GT[1];
            Map newMap = (Map)pMapClass.newInstance();
            boolean VMap=V instanceof Map;
            boolean VCollection=V instanceof Collection;
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
                Object vv;
                if(v instanceof CommentedValue){
                    tag = pMapName + "." + key;
                    vv=me.getValue();
                    ((SerializableYamlObject)(pT)).comments.put(tag, ((CommentedValue) v).getComments());
                    if(vv instanceof CommentedSection){
                        Map mv=((CommentedSection)(((CommentedValue) v).getValue())).values();
                        vv=ConvertMapObject(tag,V.getClass(),(Map<String,Object>)(mv),pT);
                    }
                }else{
                    vv=v;
                }
                if (VMap) {
                    if(Cons==null) {
                        newMap.put(key, ConvertMapObject(tag,V.getClass(),pNmap,pT));
                    }else{
                        newMap.put(Cons.newInstance(key),ConvertMapObject(tag,V.getClass(),pNmap,pT));
                    }
                }else if(VCollection) {
                    if (Cons == null) {
                        newMap.put(key, ConvertCollectionObject(V.getClass(), (Collection<Object>) v));
                    } else {
                        newMap.put(Cons.newInstance(key), ConvertCollectionObject(V.getClass(), (Collection<Object>) v));
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
    private static <L> Collection ConvertCollectionObject(Class<L> pCollectionClass,Collection<Object> pCollection){
        try{
            Type[] GT = pCollectionClass.getClass().getGenericInterfaces();
            Type V=GT[0];
            Collection newCollection=(Collection)pCollectionClass.newInstance();
            boolean VMap=V instanceof Map;
            boolean VCollection=V instanceof Collection;
            for (Object no:pCollection){
                if(VMap){
                    no=ConvertMapObject(null,no.getClass(),(Map<String, Object>) no,null);
                }else if(VCollection){
                    no=ConvertCollectionObject(no.getClass(),(Collection<Object>) no);
                }
                newCollection.add(no);
            }
            return newCollection;
        }catch (Exception e){
            try{
                return (Collection)pCollectionClass.newInstance();
            }catch (Exception ee){return null;}
        }
    }
}
