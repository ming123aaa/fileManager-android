package com.ohuang.filemanager.statedata;


import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;



import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class StateData<T> {
    volatile T value;
    Set<Observer<T>> set = new HashSet<>();
    HashMap<Observer<T>, LiveDataLifecycleObserver> lifecycleObserverMap = new HashMap<>();
    HashMap<Observer<T>, AlwaysLifecycleObserver> alwaysLifecycleObserverMap = new HashMap<>();
    volatile long version = 0;
    volatile Handler eventHandler;
    final Object object = new Object();

    final Looper looper;
    public StateData(T t) {
        this();
        this.value=t;
    }

    public StateData(){
        this(Looper.getMainLooper());
    }

    public StateData(Looper looper) {
       this.looper=looper;
    }



    public void setValue(T t) {

        synchronized (object) {
            value = t;
            version++;
            for (Observer<T> o : set) {
                o.onchange(t);
            }
            ForeachUtil.mapForeach(lifecycleObserverMap, new MapConsumer<Observer<T>, LiveDataLifecycleObserver>() {
                @Override
                public boolean accept(Observer<T> tObserver, LiveDataLifecycleObserver liveDataLifecycleObserver) {
                    liveDataLifecycleObserver.dispatchObserver();
                    return false;
                }
            });
            ForeachUtil.mapForeach(alwaysLifecycleObserverMap, new MapConsumer<Observer<T>, AlwaysLifecycleObserver>() {
                @Override
                public boolean accept(Observer<T> tObserver, AlwaysLifecycleObserver alwaysLifecycleObserver) {
                    alwaysLifecycleObserver.dispatchObserver();
                    return false;
                }
            });

        }
    }

    public void postValue(T t) {
        if (Looper.myLooper() == looper) {
            setValue(t);
        } else {
            if (eventHandler == null) {
                synchronized (object) {
                    if (eventHandler == null) {
                        eventHandler = new Handler(looper);
                    }
                }
            }
            eventHandler.post(new Runnable() {
                @Override
                public void run() {
                    setValue(t);
                }
            });
        }
    }

    public T getValue() {
        return value;
    }

    public void addObserver(Observer<T> observer) {
        set.add(observer);
    }

    /**
     * 可见状态才会回调
     * @param lifecycleOwner
     * @param observer
     */
    public void addObserver(LifecycleOwner lifecycleOwner, Observer<T> observer) {
        Lifecycle iLifecycle = lifecycleOwner.getLifecycle();
        if (!lifecycleObserverMap.containsKey(observer)) {
            LiveDataLifecycleObserver liveDataLifecycleObserver = new LiveDataLifecycleObserver(iLifecycle, observer);
            iLifecycle.addObserver(liveDataLifecycleObserver);
            lifecycleObserverMap.put(observer, liveDataLifecycleObserver);
        } else {
            throw new RuntimeException("observer  already Add");
        }
    }

    /**
     *  回调不会受生命周期影响   自动remove
     * @param lifecycleOwner
     * @param observer
     */
    public void addAlwaysObserver(LifecycleOwner lifecycleOwner, Observer<T> observer) {
        Lifecycle iLifecycle = lifecycleOwner.getLifecycle();
        if (!alwaysLifecycleObserverMap.containsKey(observer)) {
            AlwaysLifecycleObserver liveDataLifecycleObserver = new AlwaysLifecycleObserver(iLifecycle, observer);
            iLifecycle.addObserver(liveDataLifecycleObserver);
            alwaysLifecycleObserverMap.put(observer, liveDataLifecycleObserver);
        } else {
            throw new RuntimeException("observer  already Add ");
        }
    }

    public void addObserverForSticky(Observer<T> observer) {
        set.add(observer);
        if (version>0) {
            observer.onchange(value);
        }
    }

    public void addObserverForSticky(LifecycleOwner lifecycleOwner, Observer<T> observer) {
        Lifecycle lifecycle = lifecycleOwner.getLifecycle();
        if (!lifecycleObserverMap.containsKey(observer)) {
            LiveDataLifecycleObserver liveDataLifecycleObserver = new LiveDataLifecycleObserver(lifecycle, observer, true);
            lifecycle.addObserver(liveDataLifecycleObserver);
            lifecycleObserverMap.put(observer, liveDataLifecycleObserver);
            liveDataLifecycleObserver.dispatchObserver();

        } else {
            throw new RuntimeException("observer  already Add ");
        }
    }

    public void addAlwaysObserverForSticky(LifecycleOwner lifecycleOwner, Observer<T> observer) {
        Lifecycle lifecycle = lifecycleOwner.getLifecycle();
        if (!alwaysLifecycleObserverMap.containsKey(observer)) {
            AlwaysLifecycleObserver liveDataLifecycleObserver = new AlwaysLifecycleObserver(lifecycle, observer);
            lifecycle.addObserver(liveDataLifecycleObserver);
            alwaysLifecycleObserverMap.put(observer, liveDataLifecycleObserver);
            liveDataLifecycleObserver.dispatchObserver();
        } else {
            throw new RuntimeException("observer  already Add ");
        }
    }

    public void removeObserver(Observer<T> observer) {
        synchronized (object) {
            set.remove(observer);
            LiveDataLifecycleObserver liveDataLifecycleObserver = lifecycleObserverMap.remove(observer);
            if (liveDataLifecycleObserver != null) {
                liveDataLifecycleObserver.remove();
            }
            AlwaysLifecycleObserver alwaysLifecycleObserver = alwaysLifecycleObserverMap.remove(observer);
            if (alwaysLifecycleObserver != null) {
                alwaysLifecycleObserver.remove();
            }
        }
    }

    public void removeAllObserver() {
        synchronized (object) {
            set.clear();
            ForeachUtil.mapForeach(lifecycleObserverMap, new MapConsumer<Observer<T>, LiveDataLifecycleObserver>() {
                @Override
                public boolean accept(Observer<T> tObserver, LiveDataLifecycleObserver liveDataLifecycleObserver) {
                    liveDataLifecycleObserver.remove();
                    return false;
                }
            });
            lifecycleObserverMap.clear();
            ForeachUtil.mapForeach(alwaysLifecycleObserverMap, new MapConsumer<Observer<T>, AlwaysLifecycleObserver>() {
                @Override
                public boolean accept(Observer<T> tObserver, AlwaysLifecycleObserver alwaysLifecycleObserver) {
                    alwaysLifecycleObserver.remove();
                    return false;
                }
            });
            alwaysLifecycleObserverMap.clear();
        }
    }

    private class AlwaysLifecycleObserver implements LifecycleEventObserver {

        Lifecycle lifecycle;
        Observer<T> observer;


        public AlwaysLifecycleObserver(Lifecycle lifecycle, Observer<T> o) {
            this.lifecycle = lifecycle;
            this.observer = o;
        }

        public void dispatchObserver() {
            if (observer != null&&version>0) {
                observer.onchange(value);
            }
        }


        public void onStop() {
            if (observer != null) {
                alwaysLifecycleObserverMap.remove(observer);
            }
            remove();
        }

        public void remove() {
            if (lifecycle != null) {
                lifecycle.removeObserver(this);
            }
            observer = null;
            lifecycle = null;
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                onStop();
            }
        }
    }

    private class LiveDataLifecycleObserver implements LifecycleEventObserver {
        long l_version = 0;
        Lifecycle lifecycle;
        Observer<T> observer;
        volatile boolean isLive = false;

        public LiveDataLifecycleObserver(Lifecycle lifecycle, Observer<T> o) {
            this(lifecycle, o, false);
        }

        public LiveDataLifecycleObserver(Lifecycle lifecycle, Observer<T> o, boolean sticky) {
            if (sticky) {
                l_version = version - 1;
            } else {
                l_version = version;
            }
            this.lifecycle = lifecycle;
            this.observer = o;
            if (lifecycle != null) {
                isLive = lifecycle.getCurrentState().equals(Lifecycle.State.RESUMED);
            }
        }


        public void onActive() {

            isLive = true;
            dispatchObserver();
        }

        public void dispatchObserver() {
            if (isLive) {
                if (version > l_version&&version>0) {
                    if (observer != null) {
                        observer.onchange(value);
                    }
                    l_version = version;
                }
            }
        }


        public void onBackground() {

            isLive = false;
        }

        public void onStop() {

            isLive = false;
            if (observer != null) {
                lifecycleObserverMap.remove(observer);
            }
            remove();
        }

        public void remove() {
            if (lifecycle != null) {
                lifecycle.removeObserver(this);
            }
            observer = null;
            lifecycle = null;
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
            switch (event) {
                case ON_RESUME:
                    onActive();
                    break;
                case ON_DESTROY:
                    onStop();
                    break;
                case ON_STOP:
                    onBackground();
                    break;
                default:
                    break;
            }
        }
    }

}
