package cc.commons.commentedyaml.serialize;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by xjboss on 2017/5/29.
 */
public class SerializableYamlObject {
    public final HashMap<String, ArrayList<String>> getComments() {
        return comments;
    }

    final transient HashMap<String,ArrayList<String>> comments=new HashMap<>();
    public final ArrayList<String> getCommentInfo(String tag){
        return comments.get(tag);
    }
}
