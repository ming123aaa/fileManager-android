package com.ohuang.filemanager.statedata;


import java.util.HashMap;
import java.util.Map;

public class SateDataBus {
    private final Map<String, StateData<Object>> map;

    private SateDataBus() {
        map = new HashMap<>();
    }

    private static class SingletonHolder {
        private static final SateDataBus DEFAULT_BUS = new SateDataBus();
    }

    public static SateDataBus get() {
        return SingletonHolder.DEFAULT_BUS;
    }

    public <T> StateData<T> with(String key, Class<T> type) {
        if (!map.containsKey(key)) {
            map.put(key, new StateData<>());
        }
        return (StateData<T>) map.get(key);
    }

    public StateData<Object> with(String key) {
        return with(key, Object.class);
    }

    public StateData<Object> removeKey(String key) {
        return map.remove(key);
    }


}
