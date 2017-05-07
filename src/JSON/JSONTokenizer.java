package JSON;//package jsonbeans;

import sun.beanbox.Wrapper;
import sun.beanbox.WrapperEventInfo;

import java.awt.*;
import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Class used for lexical division of json object
 */

class JSONTokenizer {

    // Token types
    private static final int TYPE_INT_CONST = 1;
    private static final int TYPE_SYMBOL = 2;
    private static final int TYPE_IDENTIFIER = 3;

    private static final char LEFT_BRACE = '{';
    private static final char RIGHT_BRACE = '}';
    private static final char LEFT_BRACKET = '[';
    private static final char RIGHT_BRACKET = ']';
    private static final char COLON = ':';
    private static final char COMMA = ',';
    private static final char QUOTE = '\"';
    private static final String KW_CLASS = "class";

    StreamTokenizer tokenizer;

    private Map<Character, Character> reservedSymbols;

    private String currentToken;

    private int tokenType;

    private Map<String, Vector> events = new HashMap<>();

    JSONTokenizer(String jsonString) throws JSONDeserializationException {

        StringReader stringReader = new StringReader(jsonString);
        try{
            initializeTokenizer(stringReader);
            getNextToken();
        } catch (IOException | JSONDeserializationException e) {
            JSONError("Intialization problem", 0);
        }
    }

    private void initializeTokenizer(Reader input)
            throws IOException{
        tokenizer = new StreamTokenizer(input);

        tokenizer.parseNumbers();

        tokenizer.whitespaceChars(COMMA, COMMA);
        tokenizer.quoteChar(QUOTE);

        tokenizer.ordinaryChar(LEFT_BRACE);
        tokenizer.ordinaryChar(RIGHT_BRACE);
        tokenizer.ordinaryChar(LEFT_BRACKET);
        tokenizer.ordinaryChar(RIGHT_BRACKET);
        tokenizer.ordinaryChar(COLON);

        initSymbols();
    }

    private void initSymbols() {

        reservedSymbols = new HashMap<>();

        reservedSymbols.put(LEFT_BRACE, LEFT_BRACE);
        reservedSymbols.put(LEFT_BRACKET, LEFT_BRACKET);
        reservedSymbols.put(RIGHT_BRACE, RIGHT_BRACE);
        reservedSymbols.put(RIGHT_BRACKET, RIGHT_BRACKET);
        reservedSymbols.put(COLON, COLON);
        reservedSymbols.put(COMMA, COMMA);
        reservedSymbols.put(QUOTE, QUOTE);
    }

    private void getNextToken() throws JSONDeserializationException {
        if (!hasMoreTokens()) JSONError("Unexpected end of file", tokenizer.lineno());

        try {
            tokenizer.nextToken();
        } catch (IOException e) {
            JSONError("Something went wrong", tokenizer.lineno());
        }

        switch (tokenizer.ttype) {
            case StreamTokenizer.TT_NUMBER:
                tokenType = TYPE_INT_CONST;
                currentToken = String.valueOf(tokenizer.nval);
                break;
            case StreamTokenizer.TT_WORD:
                tokenType = TYPE_IDENTIFIER;
                currentToken = tokenizer.sval;
                break;
            case (int)QUOTE:
                tokenType = TYPE_IDENTIFIER;
                currentToken = tokenizer.sval;
                break;
            default:
                char charToken = (char) tokenizer.ttype;
                if (reservedSymbols.containsValue(charToken)) {
                    tokenType = TYPE_SYMBOL;
                    currentToken = String.valueOf(charToken);
                    break;
                } else {
                    JSONError("Unexpected token: " + charToken, tokenizer.lineno());
                }
        }
    }

    private boolean hasMoreTokens() {
        return tokenizer.ttype != StreamTokenizer.TT_EOF;
    }


    Object readObject() throws JSONDeserializationException {

        if ("null".equals(currentToken)) return null;

        if(!(tokenType == TYPE_SYMBOL && currentToken.equals(String.valueOf(LEFT_BRACE))))
            JSONError("Missing \'{\'", tokenizer.lineno());

        getNextToken();

        if(!(tokenType == TYPE_IDENTIFIER && currentToken.equals(KW_CLASS)))
            JSONError("Missing \"class\" keyword", tokenizer.lineno());

        getNextToken();

        if(!(tokenType == TYPE_SYMBOL && currentToken.equals(String.valueOf(COLON))))
            JSONError("Missing \':\'", tokenizer.lineno());

        getNextToken();

        if(!(tokenType == TYPE_IDENTIFIER))
            JSONError("Missing type identifier", tokenizer.lineno());

        String className = currentToken;

        try{
            Class<?> aClass = Class.forName(className);

            Object instance = /*aClass.newInstance();*/createInstance(aClass);

            BeanInfo beanInfo = java.beans.Introspector.getBeanInfo(aClass);

            Map<String, PropertyDescriptor> propertyMap = getPropertyMap(beanInfo.getPropertyDescriptors());

            Map<String, Object> propertyValues = new HashMap<String, Object>();

            boolean eventsSizeChanged = false;

            getNextToken();

            while (!(tokenType == TYPE_SYMBOL && currentToken.equals(String.valueOf(RIGHT_BRACE)))){

                if(!(tokenType == TYPE_IDENTIFIER))
                    JSONError("Missing type identifier", tokenizer.lineno());

                PropertyDescriptor property = propertyMap.get(currentToken);

                getNextToken();

                if (property.getName().equals("eventHookupInfo")) {
                    getNextToken();
                    eventsSizeChanged = readEventTarget(instance);

                    getNextToken();
                    continue;
                }

                if (property.getName().equals("locale")) {
                    getNextToken();

                    if(currentToken.equals("{")) {
                        defineLocale(property, instance);
                    }

                    getNextToken();
                    continue;
                }

                if(!(tokenType == TYPE_SYMBOL && currentToken.equals(String.valueOf(COLON))))
                    JSONError("Missing \':\'", tokenizer.lineno());

                if(JSONUtil.primitiveSet.contains(property.getPropertyType()))
                    readPrimitive(property, instance, propertyValues);
                else if(property instanceof IndexedPropertyDescriptor)
                    readArray((IndexedPropertyDescriptor) property, instance, propertyValues);
                else{
                    getNextToken();

                    int count = 0;
                    if (currentToken.equals("{"))
                        ++count;

                    try {
                        Object ob = readObject();
                        propertyValues.put(property.getName(), ob);
                        property.getWriteMethod().invoke(instance, ob);
                    } catch (NullPointerException e) {
                        //do nothing
//                        System.out.println("");
/*                        try {
                            Object obj = property.getReadMethod().invoke(instance);
                        } catch (Exception ex) {

                        }*/
                        System.out.println("Property: " + property.getName() + " didn't have set() method.");

                        while (count != 0)
                        {
                            if (currentToken.equals("{"))
                                ++count;

                            if (currentToken.equals("}")) {
                                --count;

                                if (count == 0)
                                    break;
                            }

                            getNextToken();
                        }
                    }
                    catch (JSONDeserializationException e2) {
                        System.out.println("Was unable to instantiate inner property: " + property.getName() + ", so skipped it.");

                        while (count != 0)
                        {
                            if (currentToken.equals("{"))
                                ++count;

                            if (currentToken.equals("}")) {
                                --count;

                                if (count == 0)
                                    break;
                            }

                            getNextToken();
                        }
                    }
                }


                getNextToken();
            }

            if (instance instanceof Wrapper) {
                instance = new Wrapper(propertyValues.get("bean"), (String)propertyValues.get("beanLabel"),
                        (String)propertyValues.get("beanName"));

                for (PropertyDescriptor p :
                     propertyMap.values()) {
                    if (p.getWriteMethod() != null && p.getValue("transient") == null) {
                        p.getWriteMethod().invoke(instance, propertyValues.get(p.getName()));
                    }
                }

                if (eventsSizeChanged) {
                    if (events.get("") != null) {
                        events.put(((Wrapper)instance).getChild().getName(), events.get(""));
                        events.remove("");
                    }
                }
            }

            if (instance instanceof Dimension) {
                double width = (Double)propertyValues.get("width");
                double height = (Double)propertyValues.get("height");
                instance = new Dimension((int)width, (int)height);
            }

            return instance;
        }
        catch (ClassNotFoundException e){
//            JSONError("Class have not found", tokenizer.lineno());
            return null;
        }
        catch (ReflectiveOperationException e){
            JSONError("Unable to instantiate class " + className, tokenizer.lineno());
            return null;
        }
        catch (IntrospectionException e){
            JSONError("Unable to introspect class " + className, tokenizer.lineno());
            return null;
        }
    }

    void readArray(IndexedPropertyDescriptor property, Object invoker, Map<String, Object> propertyValues)
            throws JSONDeserializationException, InvocationTargetException, IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        getNextToken();

        if(!(tokenType == TYPE_SYMBOL && currentToken.equals(String.valueOf(LEFT_BRACKET))))
            JSONError("Missing \'[\'", tokenizer.lineno());
        getNextToken();

        if(JSONUtil.primitiveArraysSet.contains(property.getPropertyType())){
            ArrayList<String> tokensList = new ArrayList<>();

            while (!(tokenType == TYPE_SYMBOL && currentToken.equals(String.valueOf(RIGHT_BRACKET)))){
                tokensList.add(currentToken);

                getNextToken();
            }

            deserializeArray(property, invoker, tokensList, propertyValues);
        }
        else {
            ArrayList<Object> objects = new ArrayList<>();

            while (!(tokenType == TYPE_SYMBOL && currentToken.equals(String.valueOf(RIGHT_BRACKET)))){
                objects.add(property.getIndexedPropertyType().cast(readObject()));
                getNextToken();
            }

            Object[] arr = (Object[]) Array.newInstance(property.getIndexedPropertyType(), objects.size());

            for (int i = 0; i < objects.size(); i++) {
                arr[i] = objects.get(i);
            }

            try {
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } catch (NullPointerException e) {
                //do nothing
            }
        }
    }

    @SuppressWarnings({"Duplicates", "PrimitiveArrayArgumentToVarargsMethod", "ConfusingArgumentToVarargsMethod"})
    void deserializeArray(IndexedPropertyDescriptor property, Object invoker, ArrayList<String> tokensList,
                          Map<String, Object> propertyValues)
            throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {

        Class<?> propertyType = property.getPropertyType();
        int arrSize = tokensList.size();

        try {
            if (propertyType == int[].class) {
                int[] arr = new int[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).intValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, arr);
            } else if (propertyType == short[].class) {
                short[] arr = new short[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).shortValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, arr);
            } else if (propertyType == byte[].class) {
                byte[] arr = new byte[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).byteValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, arr);
            } else if (propertyType == long[].class) {
                long[] arr = new long[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).longValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, arr);
            } else if (propertyType == double[].class) {
                double[] arr = new double[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i));
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, arr);
            } else if (propertyType == float[].class) {
                float[] arr = new float[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).floatValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, arr);
            } else if (propertyType == char[].class) {
                char[] arr = new char[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = tokensList.get(i).charAt(0);
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, arr);
            } else if (propertyType == boolean[].class) {
                boolean[] arr = new boolean[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Boolean.valueOf(tokensList.get(i));
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, arr);
            } else if (propertyType == Integer[].class) {
                Integer[] arr = new Integer[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).intValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } else if (propertyType == Byte[].class) {
                Byte[] arr = new Byte[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).byteValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } else if (propertyType == Short[].class) {
                Short[] arr = new Short[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).shortValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } else if (propertyType == Long[].class) {
                Long[] arr = new Long[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).longValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } else if (propertyType == Double[].class) {
                Double[] arr = new Double[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i));
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } else if (propertyType == Float[].class) {
                Float[] arr = new Float[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Double.valueOf(tokensList.get(i)).floatValue();
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } else if (propertyType == Character[].class) {
                Character[] arr = new Character[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = tokensList.get(i).charAt(0);
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } else if (propertyType == Boolean[].class) {
                Boolean[] arr = new Boolean[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Boolean.valueOf(tokensList.get(i));
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } else if (propertyType == String[].class) {
                String[] arr = (String[]) Array.newInstance(String.class, tokensList.size());
                for (int i = 0; i < tokensList.size(); i++) {
                    arr[i] = tokensList.get(i);
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            } else if (propertyType == Class[].class) {
                Class[] arr = new Class[arrSize];
                for (int i = 0; i < arrSize; i++) {
                    arr[i] = Class.forName(tokensList.get(i));
                }
                propertyValues.put(property.getName(), arr);
                property.getWriteMethod().invoke(invoker, (Object) arr);
            }
        } catch (NullPointerException e) {
            //do nothing
        }
    }

    void readPrimitive(PropertyDescriptor property, Object instance, Map<String, Object> propertyValues)
            throws ReflectiveOperationException, JSONDeserializationException{

        getNextToken();

        Class<?> propertyType = property.getPropertyType();
        //propertyType.getField("").setAccessible(true); // !!!

        try {
            if (JSONUtil.numberTypes.contains(propertyType)) {

                if (tokenType != TYPE_INT_CONST) JSONError("Wrong value!", tokenizer.lineno());

                Number value = 0;

                if (propertyType == Byte.class || propertyType == byte.class)
                    value = Double.valueOf(currentToken).byteValue();

                else if (propertyType == Short.class || propertyType == short.class)
                    value = Double.valueOf(currentToken).shortValue();

                else if (propertyType == Integer.class || propertyType == int.class)
                    value = Double.valueOf(currentToken).intValue();

                else if (propertyType == Long.class || propertyType == long.class)
                    value = Double.valueOf(currentToken).longValue();

                else if (propertyType == Float.class || propertyType == float.class)
                    value = Double.valueOf(currentToken).floatValue();

                else if (propertyType == Double.class || propertyType == double.class)
                    value = Double.valueOf(currentToken);

                propertyValues.put(property.getName(), value);
                property.getWriteMethod().invoke(instance, value);
            } else if (JSONUtil.characterSequenceTypes.contains(propertyType)) {
                if (propertyType == Character.class || propertyType == char.class) {
                    propertyValues.put(property.getName(), currentToken.charAt(0));
                    property.getWriteMethod().invoke(instance, currentToken.charAt(0));
                }
                else {
                    propertyValues.put(property.getName(), currentToken);
                    property.getWriteMethod().invoke(instance, currentToken);
                }
            } else if (JSONUtil.logicalTypes.contains(propertyType)) {
                propertyValues.put(property.getName(), Boolean.valueOf(currentToken));
                property.getWriteMethod().invoke(instance, Boolean.valueOf(currentToken));
            }
            else if (propertyType == Class.class) {
                propertyValues.put(property.getName(), Class.forName(currentToken));
                property.getWriteMethod().invoke(instance, Class.forName(currentToken));
            }
        } catch (NullPointerException e) {
            //do nothing

        }
    }

    private Map<String, PropertyDescriptor> getPropertyMap(PropertyDescriptor[] descriptors) {

        HashMap<String, PropertyDescriptor> propertyDescriptorHashMap = new HashMap<>();

        for(PropertyDescriptor descriptor: descriptors){
            propertyDescriptorHashMap.put(descriptor.getName(), descriptor);
        }

        return propertyDescriptorHashMap;
    }

    private void JSONError(String message, int line) throws JSONDeserializationException {
        throw new JSONDeserializationException(message, line);
    }

    boolean readEventTarget(Object invoker) throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, ClassNotFoundException, JSONDeserializationException {
        getNextToken();

        Vector v = new Vector();
        while (!currentToken.equals("]")) {
            try {
                //Object obj = readObject();
                //WrapperEventInfo eventInfo = (WrapperEventInfo)obj;
                if (invoker instanceof Wrapper) {
//                    ((Wrapper)invoker).add

                    for (int i = 0; i < 6; ++i) { getNextToken(); }
                    String adaptorClassName = currentToken;

                    for (int i = 0; i < 3; ++i) { getNextToken(); }
                    String eventSetName = currentToken;

                    for (int i = 0; i < 3; ++i) { getNextToken(); }

                    Object targetBean = readObject();


/*                    Class<?> wr = invoker.getClass();
                    Method add = wr.getDeclaredMethod("addEventTarget", String.class, wr, Object.class);
                    add.setAccessible(true);

                    Class<?> hookUp = Class.forName(adaptorClassName);
                    Object listener = hookUp.newInstance();
                    Method[] all = hookUp.getDeclaredMethods();
                    Method set = null;
                    for (int i = 0; i < all.length; ++i) {
                        if (all[i].getName().equals("setTarget"))
                            set = all[i];
                    }
                    set.invoke(listener, targetBean);

                    add.invoke(invoker, eventSetName, new Wrapper(targetBean, null, null), listener);*/
                    v.addElement(new WrapperEventInfo(targetBean, adaptorClassName, eventSetName));

                    for (int i = 0; i < 2; ++i) { getNextToken(); }
                }
            } catch (JSONDeserializationException e) {

            }
        }

        if (!v.isEmpty()) {
            events.put("", v);
            return true;
        }

        return false;
    }

    public Map<String, Vector> getEvents() { return events; }

    void defineLocale(PropertyDescriptor property, Object invoker) throws JSONDeserializationException,
    IllegalAccessException, InvocationTargetException{
//        property.getWriteMethod().invoke(invoker, new Locale("ru"));
        getNextToken();

        int count = 1;
        while (count != 0)
        {
            if (currentToken.equals("{"))
                ++count;

            if (currentToken.equals("}")) {
                --count;

                if (count == 0)
                    break;
            }

            getNextToken();
        }
    }

    private Object createInstance(Class<?> className)throws InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException
    {
//        className.cast()
        Constructor<?>[] constrArr = className.getDeclaredConstructors();
        boolean hasDefault;
        //TODO: Best fit constructor
        int best_fit = 0;
//        int count = 0;

        if (constrArr.length == 0)
            hasDefault = true;
        else
            hasDefault = false;

        for (int i = 0; i < constrArr.length; ++i) {
            int curr_type_len = constrArr[i].getGenericParameterTypes().length;
            if (curr_type_len == 0) {
                hasDefault = true;
                break;
            }
            else {
                int count = 0;
                for (int m = 0; m < curr_type_len; ++m) {
                    if (JSONUtil.primitiveSet.contains(constrArr[i].getGenericParameterTypes()[m].getClass()))
                        ++count;
                }

                if (count == curr_type_len)
                    best_fit = i;
            }
        }

        Object instance = null;

        if (hasDefault) {
            Constructor<?> constr = className.getDeclaredConstructor();
            constr.setAccessible(true);

            instance = constr.newInstance();
        }
        else
        {
//            Constructor<?> con = className.getDeclaredConstructor(Object.class);
            Class<?>[] c = constrArr[best_fit].getParameterTypes();
            Object[] o = new Object[c.length];
//            Class[] obj = new Class[c.length];
            constrArr[best_fit].setAccessible(true);

            for (int j = 0; j < c.length; ++j) {
                if (JSONUtil.primitiveSet.contains(c[j]))
                {
                    if (JSONUtil.numberTypes.contains(c[j]))
                        o[j] = 0;
                    else if (JSONUtil.characterSequenceTypes.contains(c[j]))
                        o[j] = "";
                    else
                        o[j] = false;
                }
                else
                    o[j] = createInstance(c[j]);

//                obj[j] = Object.class;
            }

            instance = constrArr[best_fit].newInstance(o);
        }

        return instance;
    }
}