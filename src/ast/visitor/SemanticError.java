package ast.visitor;

public class SemanticError extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int lineNumber;
    
    public SemanticError(String message, int lineNumber) {
        super(message);
        this.lineNumber = lineNumber;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    @Override
    public String getMessage() {
        return super.getMessage();
    }
} 