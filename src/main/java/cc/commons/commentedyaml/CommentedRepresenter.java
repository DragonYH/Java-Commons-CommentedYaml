package cc.commons.commentedyaml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

public class CommentedRepresenter extends Representer{

    /** 实现Serializable接口的类的类型标记 */
    public static String SerializableMark="===";
    /** 实现MapSerialize接口的类的类型标记 */
    public static String MapSerializeMark="==";

    public CommentedRepresenter(){
        this.multiRepresenters.put(CommentedSection.class,new CommentedSectionRepresent());
        this.multiRepresenters.put(CommentedValue.class,new CommentedValueRepresent());
        this.multiRepresenters.put(Serializable.class,new RepresentSerializable());
    }

    public Represent getNullRepresent(){
        return this.nullRepresenter;
    }

    public Map<Class<?>,Represent> getRepresents(){
        return this.representers;
    }

    public Map<Class<?>,Represent> getMultiRepresents(){
        return this.multiRepresenters;
    }

    public class RepresentSerializable extends RepresentJavaBean{

        @Override
        public Node representData(Object pData){
            MappingNode tNode=(MappingNode)super.representData(pData);
            tNode.setTag(Tag.MAP);

            Represent tStringRepresent=CommentedRepresenter.this.getRepresents().get(String.class);
            List<NodeTuple> tWarpFields=new ArrayList<>();
            tWarpFields.add(new NodeTuple(
                    tStringRepresent.representData(SerializableMark),
                    tStringRepresent.representData(pData.getClass().getName())));
            tWarpFields.addAll(tNode.getValue());
            tNode.setValue(tWarpFields);

            return tNode;
        }
    }

    public class CommentedSectionRepresent extends RepresentMap{

        @Override
        public Node representData(Object pData){
            Map<String,CommentedValue> tMapValues=((CommentedSection)pData).values();
            List<NodeTuple> tNodes=new ArrayList<NodeTuple>(tMapValues.size());
            MappingNode tNode=new MappingNode(Tag.MAP,tNodes,null);
            representedObjects.put(objectToRepresent,tNode);
            boolean tBestStyle=true;
            for(Map.Entry<String,CommentedValue> sEntry : tMapValues.entrySet()){
                if(sEntry.getValue().getValue()==null)
                    continue;

                Node tNodeKey=CommentedRepresenter.this.representData(sEntry.getKey());
                Node tNodeValue=CommentedRepresenter.this.representData(sEntry.getValue());
                if(!(tNodeKey instanceof ScalarNode&&((ScalarNode)tNodeKey).getStyle()==null)){
                    tBestStyle=false;
                }
                if(!(tNodeValue instanceof ScalarNode&&((ScalarNode)tNodeValue).getStyle()==null)){
                    tBestStyle=false;
                }
                tNodes.add(new NodeTuple(tNodeKey,tNodeValue));
            }
            if(defaultFlowStyle!=FlowStyle.AUTO){
                tNode.setFlowStyle(defaultFlowStyle.getStyleBoolean());
            }else{
                tNode.setFlowStyle(tBestStyle);
            }
            return tNode;
        }
    };

    public class CommentedValueRepresent implements Represent{

        @Override
        public Node representData(Object pData){
            return CommentedRepresenter.this.representData(((CommentedValue)pData).getValue());
        }
    };

}
