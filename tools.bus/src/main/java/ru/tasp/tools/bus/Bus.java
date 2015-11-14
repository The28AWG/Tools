package ru.tasp.tools.bus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

import ru.tasp.tools.Threads;
import ru.tasp.tools.logger.L;

/**
 * Created by the28awg on 30.10.15.
 */
public class Bus {

    private LinkedList<Handler> handlers = new LinkedList<>();

    private Bus() {
    }

    public static Bus get() {
        return Holder.BUS;
    }

    private static String argumentTypesToString(Class<?>[] argTypes) {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        if (argTypes != null) {
            for (int i = 0; i < argTypes.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                Class<?> c = argTypes[i];
                buf.append((c == null) ? "null" : c.getName());
            }
        }
        buf.append(")");
        return buf.toString();
    }

    private static String argumentObjectToString(Object[] argTypes) {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        if (argTypes != null) {
            for (int i = 0; i < argTypes.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                Class<?> c = argTypes[i].getClass();
                buf.append((c == null) ? "null" : c.getName());
            }
        }
        buf.append(")");
        return buf.toString();
    }

    public void register(Object o) {
        for (Handler handler : handlers) {
            if (handler.equalsObject(o)) {
                return;
            }
        }
        Method[] methods = o.getClass().getDeclaredMethods();
        LinkedList<MethodHandler> handlers = new LinkedList<>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Subscribe subscribe = method.getAnnotation(Subscribe.class);
                String identifier = subscribe.value();
                if (identifier.isEmpty()) {
                    identifier = argumentTypesToString(method.getParameterTypes());
                }
                handlers.add(new MethodHandler(identifier, method.getName(), subscribe.ui()));

            }
        }
        if (handlers.size() != 0) {
            L.trace("Object = %s, handlers.size(%s)", o, handlers.size());
            this.handlers.add(new Handler(o, handlers));
        }
    }

    public void unRegister(Object o) {
        Handler tmp = null;
        for (Handler handler : handlers) {
            if (handler.equalsObject(o)) {
                tmp = handler;
            }
        }
        if (tmp != null) {
            L.trace("Object = %s, Handler = %s", o, tmp);
            handlers.remove(tmp);
        }
    }

    public void fire(Object... o) {
        for (Handler handler : handlers) {
            //System.out.println(argumentObjectToString(o));
            handler.fire(argumentObjectToString(o), o);
        }
    }

    private static class Holder {
        static final Bus BUS = new Bus();
    }

    private class Handler {
        private Object object;
        private LinkedList<MethodHandler> handlers;

        public Handler(Object object, LinkedList<MethodHandler> handlers) {
            this.object = object;
            this.handlers = handlers;
        }

        public void fire(String identifier, final Object... o) {
            for (final MethodHandler handler : handlers) {
                if (handler.getIdentifier().equals(identifier)) {
                    L.trace("identifier = %s", identifier);
                    if (handler.isUi()) {
                        Threads.get().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                invoke(handler.getMethod(), o);
                            }
                        });
                    } else {
                        invoke(handler.getMethod(), o);
                    }
                }
            }
        }

        public void invoke(String sMethod, Object... o) {
            try {
                for (Method method : object.getClass().getDeclaredMethods()) {
                    if (method.getName().equals(sMethod)) {
                        method.setAccessible(true);
                        method.invoke(object, o);
                    }
                }
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        public boolean equalsObject(Object obj) {
            return object.equals(obj);
        }
    }

    private class MethodHandler {
        private String method;
        private String identifier;
        private boolean ui;

        public MethodHandler(String identifier, String method, boolean ui) {
            this.method = method;
            this.identifier = identifier;
            this.ui = ui;
        }

        public String getMethod() {
            return method;
        }

        public String getIdentifier() {
            return identifier;
        }

        public boolean isUi() {
            return ui;
        }
    }
}