package JSON;//package jsonbeans;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Class to read Beans from JSON
 */
public class JSONDecoder{

    private JSONTokenizer tokenizer;

    private Map<String, Vector> events = new HashMap<>();

    /**
     * @param jsonString - A string with JSON representation of object
     * @throws JSONDeserializationException - Deserialization problem
     * encapsulated in exception
     */
    public JSONDecoder(String jsonString) throws JSONDeserializationException {
        tokenizer = new JSONTokenizer(jsonString);
    }

    /**
     * @return deserialized object
     * @throws JSONDeserializationException  - Deserialization problem
     * encapsulated in exception
     */
    public Object readJSON() throws JSONDeserializationException {
        Object ob = tokenizer.readObject();
        events = tokenizer.getEvents();
        return ob;
    }

    public Map<String, Vector> getEvents() { return events; }
}