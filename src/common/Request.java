package common;

import java.io.Serializable;

/**
 * This class is used to serialize/deserialize the request.
 */
public class Request implements Serializable {
    public enum Method {
        PUT,
        GET,
        DELETE
    }

    private Method method;
    private String key;
    private String value;

    public Method getMethod() {
        return method;
    }
    public String getKey() {
        return key;
    }
    public String getValue() {
        return value;
    }

    /**
     * Constructor for the request.
     *
     * @param method
     * @param key
     * @param value
     */
    public Request (Method method, String key, String value) {
        this.method = method;
        this.key = key;
        this.value = value;
    }

    /**
     * Validate the request input based on different methods.
     * If it's PUT, then it should contain both key and value.
     * If it's GET or DELETE, then it should only contain a key.
     *
     * @param request
     * @return
     */
    private static boolean isValid(Request request) {
        if (request.getMethod() == Method.PUT) {
            if (request.key == null || request.value == null) {
                return false;
            }
        } else if (request.getMethod() == Method.GET || request.getMethod() == Method.DELETE) {
            if (request.key == null || request.value != null) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Serialize the request.
     * Print out the request in a format of "method: , key: , value: ".
     * For GET or DELETE, there are no values.
     *
     * @return
     */
    @Override
    public String toString() {
        String res = String.format("method: %s, key: %s", method.toString(), key);
        if (value != null) {
            res += ", value: " + value;
        }
        return res;
    }

    /**
     * Deserialize the message (string) and then convert it to a Request object.
     * Based on the format we set.
     *
     * @param input
     * @return
     */
    public static Request createRequest(String input) {
        if (input == null || input.length() == 0) {
            throw new IllegalArgumentException("The input is invalid.");
        }

        // The input should be in a format of "method, key" or "method, key, value".
        String[] parts = input.trim().split(",");
        if (parts.length != 2 && parts.length != 3) {
            throw new IllegalArgumentException("Malformed request with " + parts.length + " parts.");
        }

        // Parse the string.
        Method method = null;
        String key = null;
        String val = null;
        for (String part : parts) {
            String[] pair = part.trim().split(":");
            // It should contain the name and the corresponding value.
            if (pair.length != 2) {
                throw new IllegalArgumentException("The value of the element is missing.");
            }
            if (pair[0].trim().equals("method")) {
                method = Method.valueOf(pair[1].trim());
            } else if (pair[0].trim().equals("key")) {
                key = pair[1].trim();
            } else if (pair[0].trim().equals("value")){
                val = pair[1].trim();
            }
        }
        Request result = new Request(method, key, val);

        // Validate the request before finishing.
        if (!isValid(result)) {
            throw new IllegalArgumentException("Malformed request with Syntax error.");
        }
        return result;
    }
}
