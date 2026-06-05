package com.ohuang.filemanager.statedata;

import androidx.annotation.NonNull;



import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

 class ForeachUtil {

    public static <T,U> void mapForeach(@NonNull Map<T,U> map, MapConsumer<T,U> mapConsumer){

        Set<T> set=map.keySet();
        Iterator<T> iterator=set.iterator();
        while (iterator.hasNext()){
           T t= iterator.next();
           U u= map.get(t);
           if (mapConsumer.accept(t,u)){
               break;
           }
        }
    }

    public static <T> void listForeach(@NonNull List<T> list, Consumer<T> consumer){
        for (int i = 0; i < list.size(); i++) {
            if (consumer.accept(list.get(i))){
                break;
            }
        }
        
    }
}


