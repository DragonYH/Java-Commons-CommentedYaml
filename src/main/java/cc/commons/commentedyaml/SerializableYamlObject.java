package cc.commons.commentedyaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xjboss on 2017/5/29.
 */
public class SerializableYamlObject {
    final transient HashMap<String,ArrayList<String>> comments=new HashMap<>();
    public final ArrayList<String> getCommentInfo(String tag){
        return comments.get(tag);
    }
}
