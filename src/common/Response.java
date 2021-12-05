package common;

import java.io.Serializable;

/**
 * This class is used to serialize/deserialize the response.
 */
public class Response implements Serializable {
    public enum Status {
        SUCCEED,
        FAILED
    }

    private String code;
    private Status status;
    private String value;

    public Status getStatus() {
        return status;
    }
    public String getValue() {
        return value;
    }

    /**
     * Constructor for the response.
     *
     * @param status
     * @param value
     */
    public Response (String code, Status status, String value) {
        this.code = code;
        this.status = status;
        this.value = value;
    }

    // Convert response object to string

    /**
     * Serialize the response.
     * Print out the response in a format of "code: , operation: , value: ".
     * For PUT or DELETE, there are no values.
     *
     * @return
     */
    @Override
    public String toString() {
        String res = "code: " + code + ", operation: " + status;
        if (value != null) {
            res += ", value: " + value;
        }
        return res;
    }

    /**
     * Deserialize the message (string) and then convert it to a Response object.
     * Based on the format we set.
     *
     * @param input
     * @return
     */
    public static Response createResponse(String input) {
        String code = null;
        Status status = null;
        String val = null;
        if (input == null || input.length() == 0) {
            throw new IllegalArgumentException("The input is invalid.");
        }

        // The input should be in a format of "code, operation" or "code, operation, value".
        String[] parts = input.trim().split(",");
        if (parts.length != 2 && parts.length != 3) {
            throw new IllegalArgumentException("Malformed response with " + parts.length + " parts");
        }

        code = parts[0].trim().split(":")[1].trim();
        status = Status.valueOf(parts[1].trim().split(":")[1].trim());
        if (parts.length == 2) {
            val = parts[1].trim().split(": ")[1].trim();
        }
        return new Response(code, status, val);
    }
}
