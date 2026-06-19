package istat.android.data.access.sqlite.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;

/**
 * Created by istat on 31/10/16.
 */

public class Toolkit {
    public static boolean isEmpty(Object obj) {
        return obj == null || obj.toString().length() == 0;
    }

    public static final Class<?> getGenericTypeClass(Class<?> baseClass, int genericIndex) {
        try {
            String className = ((ParameterizedType) baseClass
                    .getGenericSuperclass()).getActualTypeArguments()[genericIndex]
                    .toString().replaceFirst("class", "").trim();
            Class<?> clazz = Class.forName(className);
            return clazz;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Class is not parametrized with generic type!!! Please use extends <> ");
        }
    }

    public static List<Field> getAllFieldFields(Class<?> cLass, boolean includeSuper, boolean acceptStatic) {
        return getAllFieldFields(cLass, true, includeSuper, acceptStatic);
    }

    public static List<Field> getAllFieldFields(Class<?> cLass, boolean includingPrivate, boolean includingSuper, boolean acceptStatic) {
        if (includingSuper) {
            return getAllFieldIncludingPrivateAndSuper(cLass, !includingPrivate, acceptStatic);
        } else {
            List<Field> fields = new ArrayList<Field>();
            Field[] tmp = cLass.getDeclaredFields();
            for (Field f : tmp) {
                if (f != null && (f.toString().contains("static") && !acceptStatic)) {
                    continue;
                }
                if (!includingPrivate || f.isAccessible()) {
                    fields.add(f);
                }
            }
            return fields;
        }
    }

    public static List<Field> getAllFieldIncludingPrivateAndSuper(Class<?> cLass) {
        return getAllFieldIncludingPrivateAndSuper(cLass, true, false);
    }

    public static List<Field> getAllFieldIncludingPrivateAndSuper(Class<?> cLass, boolean accessibleOnly) {
        return getAllFieldIncludingPrivateAndSuper(cLass, accessibleOnly, false);
    }

    public static List<Field> getAllFieldIncludingPrivateAndSuper(Class<?> cLass, boolean accessibleOnly, boolean acceptStatic) {
        List<Field> fields = new ArrayList<Field>();
        while (/*!cLass.equals(Object.class) ||*/!cLass.getCanonicalName().startsWith("java")) {
            for (Field field : cLass.getDeclaredFields()) {
                if (field != null && (field.toString().contains("static") && !acceptStatic)) {
                    continue;
                }
                if (!accessibleOnly || field.isAccessible()) {
                    fields.add(field);
                }
            }
            cLass = cLass.getSuperclass();
        }
        return fields;
    }

    public static boolean isJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return false;
        }
        return json.matches("(^\\{.*\\}$)|(^\\[.*\\]$)");
    }

    public static boolean isJsonArray(String json) {
        return json.matches("(^\\[.*\\]$)");
    }

    public static boolean isJsonObject(String json) {
        return json.matches("(^\\{.*\\}$)");
    }

    public static <T> T newInstance(Class<T> cLass) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        try {
            return cLass.newInstance();
        } catch (Exception e) {
            Constructor<T> constructor = cLass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();

        }
    }

    public final static String[] fetchPackageClass(Context context, String packageName) {
        ArrayList<String> classes = new ArrayList<String>();
        try {
            String packageCodePath = context.getPackageCodePath();
            DexFile df = new DexFile(packageCodePath);
            for (Enumeration<String> iterator = df.entries(); iterator.hasMoreElements(); ) {
                String className = iterator.nextElement();
                if (className.contains(packageName)) {
                    classes.add(className);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classes.toArray(new String[classes.size()]);
    }

    public final static Class<?> getFieldTypeClass(Field field) {
        return (Class<?>) getFieldType(field);
    }

    public final static Type getFieldType(Field field) {
        Type type;
        try {
            type = field.getGenericType();
            Log.d("asInstance", "onTRY=" + type);
        } catch (Exception e) {
            type = field.getType();
            Log.d("asInstance", "onCatch=" + type);
        }
        return type;
    }

//    public final static List<Class> getClassesForPackage(String packageName) throws ClassNotFoundException {
//        // This will hold a list of directories matching the pckgname. There may be more than one if a package is split over multiple jars/paths
//        ArrayList<File> directories = new ArrayList<File>();
//        String packageToPath = packageName.replace('.', '/');
//        try {
//            ClassLoader cld = Thread.currentThread().getContextClassLoader();
//            if (cld == null) {
//                throw new ClassNotFoundException("Can't get class loader.");
//            }
//
//            // Ask for all resources for the packageToPath
//            Enumeration<URL> resources = cld.getResources(packageToPath);
//            while (resources.hasMoreElements()) {
//                directories.add(new File(URLDecoder.decode(resources.nextElement().getPath(), "UTF-8")));
//            }
//        } catch (NullPointerException x) {
//            throw new ClassNotFoundException(packageName + " does not appear to be a valid package (Null pointer exception)");
//        } catch (UnsupportedEncodingException encex) {
//            throw new ClassNotFoundException(packageName + " does not appear to be a valid package (Unsupported encoding)");
//        } catch (IOException ioex) {
//            throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + packageName);
//        }
//
//        ArrayList<Class> classes = new ArrayList<Class>();
//        // For every directoryFile identified capture all the .class files
//        while (!directories.isEmpty()) {
//            File directoryFile = directories.remove(0);
//            if (directoryFile.exists()) {
//                // Get the list of the files contained in the package
//                File[] files = directoryFile.listFiles();
//
//                for (File file : files) {
//                    // we are only interested in .class files
//                    if ((file.getName().endsWith(".class")) && (!file.getName().contains("$"))) {
//                        // removes the .class extension
//                        int index = directoryFile.getPath().indexOf(packageToPath);
//                        String packagePrefix = directoryFile.getPath().substring(index).replace('/', '.');
//                        ;
//                        try {
//                            String className = packagePrefix + '.' + file.getName().substring(0, file.getName().length() - 6);
//                            classes.add(Class.forName(className));
//                        } catch (NoClassDefFoundError e) {
//                            // do nothing. this class hasn't been found by the loader, and we don't care.
//                        }
//                    } else if (file.isDirectory()) { // If we got to a subdirectory
//                        directories.add(new File(file.getPath()));
//                    }
//                }
//            } else {
//                throw new ClassNotFoundException(packageName + " (" + directoryFile.getPath() + ") does not appear to be a valid package");
//            }
//        }
//        return classes;
//    }

//    public static List<Class> fetchPackageClass(String packageName) {
//        List<Class> myTypes = new ArrayList();
//        Reflections reflections = new Reflections(packageName);
//        for (String s : reflections.getStore().get(SubTypesScanner.class).values()) {
//            myTypes.add(Class.forName(s));
//        }
//        return myTypes;
//    }
}
