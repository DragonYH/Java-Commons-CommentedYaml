package cc.commons.commentedyaml;

import java.util.Map;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

public class CommentedRepresenter extends Representer{

    public CommentedRepresenter(){
        this.multiRepresenters.put(CommentedSection.class,new CommentedSectionRepresent());
        this.multiRepresenters.put(CommentedValue.class,new CommentedValueRepresent());
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

    public class CommentedSectionRepresent extends RepresentMap{
        @Override
        public Node representData(Object pData){
            return super.representData(((CommentedSection)pData).getValues(false));
        }
    };

    public class CommentedValueRepresent implements Represent{

        @Override
        public Node representData(Object pData){
            return CommentedRepresenter.this.representData(((CommentedValue)pData).getValue());
        }
    };

}
