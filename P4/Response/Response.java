package Response;

public class Response {
    private int statusCode;
    private String message;
    private String content;

    public Response (int statusCode, String message, String content) {
        this.statusCode = statusCode;
        this.message = message;
        this.content = content;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public String getContent() {
        return content;
    }
}
