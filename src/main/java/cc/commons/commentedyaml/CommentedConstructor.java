package cc.commons.commentedyaml;

import java.lang.reflect.Field;
import java.util.Map;

import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

public class CommentedConstructor extends Constructor{

    public CommentedConstructor(){
        this.yamlConstructors.put(Tag.MAP,new ConstructCustomObject());
    }

    public class ConstructCustomObject extends ConstructYamlMap{

        @Override
        public Object construct(Node node){
            if(node.isTwoStepsConstruction()){
                throw new YAMLException("Unexpected referential mapping structure. Node: "+node);
            }

            Map<?,?> tRawData=(Map<?,?>)super.construct(node);
            Object tObj=tRawData.get(CommentedRepresenter.SerializableMark);
            if(tObj==null){
                return tRawData;
            }

            try{
                Class<?> tClazz=Class.forName(String.valueOf(tObj));
                java.lang.reflect.Constructor<?> tConstruct=tClazz.getDeclaredConstructor();
                tConstruct.setAccessible(true);
                tObj=tConstruct.newInstance();

                for(Map.Entry<?,?> sEntry : tRawData.entrySet()){
                    String tFieldName=String.valueOf(sEntry.getKey());
                    if(tFieldName.equals(CommentedRepresenter.SerializableMark))
                        continue;

                    Field tField=null;
                    Class<?> tSClazz=tClazz;
                    while(tField==null&&tSClazz!=null&&tSClazz!=Object.class){
                        for(Field sField : tSClazz.getDeclaredFields()){
                            if(sField.getName().equals(tFieldName)){
                                tField=sField;
                                break;
                            }
                        }
                    }
                    if(tField==null)
                        continue;

                    tField.setAccessible(true);
                    tField.set(tObj,sEntry.getValue());
                }
                return tObj;
            }catch(Throwable exp){
                throw new YAMLException("Could not deserialize object",exp);
            }

        }

        @Override
        public void construct2ndStep(Node node,Object object){
            throw new YAMLException("Unexpected referential mapping structure. Node: "+node);
        }
    }

}
