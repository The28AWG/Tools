package ru.tasp.tools.configuration;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import ru.tasp.tools.Tools;

/**
 * Created by the28awg on 25.09.15.
 */
public class Configuration implements InvocationHandler {

    private Context context = null;

    private Configuration() {
        this(Tools.getContext());
    }

    private Configuration(Context context) {
        this.context = context;
    }

    public static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(Tools.getContext());
    }

    public static <T> T proxy(Class<T> tClass) {
        return (T) Proxy.newProxyInstance(
                Tools.getContext().getClassLoader(),
                new Class[]{tClass},
                new Configuration()
        );
    }

    public static <T> T proxy(Class<T> tClass, Context context) {
        return (T) Proxy.newProxyInstance(
                Tools.getContext().getClassLoader(),
                new Class[]{tClass},
                new Configuration(context)
        );
    }

    public static void dump(Appendable output) throws IOException {
        output.append("Dumping shared preferences...\n");
        for (Map.Entry<String, ?> entry : getPreferences().getAll().entrySet()) {
            Object val = entry.getValue();
            if (val == null) {
                output.append(String.format("%s = <null>%n", entry.getKey()));
            } else {
                output.append(String.format("%s = %s (%s)%n", entry.getKey(), String.valueOf(val), val.getClass()
                        .getSimpleName()));
            }
        }
        output.append("Dump complete\n");
    }

    private SharedPreferences preferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String tmp = method.getName().toLowerCase();
        if (tmp.length() > 3) {
            String action = tmp.substring(0, 3);
            String property = tmp.substring(3);
            String namespace = method.getDeclaringClass().getCanonicalName().toLowerCase();
            String prefix = "";
            if (args.length == 2) {
                String tmpPrefix = (String) args[1];
                prefix = tmpPrefix + ".";
            }
            Object defaultValue = null;
            if (args.length >= 1) {
                defaultValue = args[0];
            }
            if (action.equalsIgnoreCase("get")) {
                if (preferences().contains(namespace + "." + prefix + property)) {
                    return preferences().getAll().get(namespace + "." + prefix + property);
                } else {
                    return defaultValue;
                }
            } else if (action.equalsIgnoreCase("set")) {
                if (args.length > 0) {
                    if (method.getParameterTypes().length > 0) {
                        String parameter = method.getParameterTypes()[0].getSimpleName();
                        switch (parameter) {
                            case "int":
                                preferences().edit().putInt(namespace + "." + prefix + property, (int) defaultValue).commit();
                                break;
                            case "String":
                                preferences().edit().putString(namespace + "." + prefix + property, (String) defaultValue).commit();
                                break;
                            case "boolean":
                                preferences().edit().putBoolean(namespace + "." + prefix + property, (boolean) defaultValue).commit();
                                break;
                            case "float":
                                preferences().edit().putFloat(namespace + "." + prefix + property, (float) defaultValue).commit();
                                break;
                            case "long":
                                preferences().edit().putLong(namespace + "." + prefix + property, (long) defaultValue).commit();
                                break;
                        }
                    }
                }
            }
        }
        return null;
    }
}