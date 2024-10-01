package collections.lesson.other;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Interview {
    // Just general understanding for the Data class - only next and hasNext visible
    class Data {
        // some hidden internal representation of data
        Object next(){
            return new Object();
        }

        boolean hasNext(){
            return true;
        }

    }
    final int MAX_CAPACITY = 5000;
    // method collection/list with data
    List<Object> getData(Data data){
        List<Object> returnData = new ArrayList<>();
        while (data.hasNext()){
            Object element = data.next();
            returnData.add(element);
            if (returnData.size()==MAX_CAPACITY){
                returnData = new LinkedList<>(returnData);
            }
        }
        return returnData;
    }


}
