package ast;

public class ClassType extends Type {
	public String className;

	public ClassType(int line, String className) {
		super(line);
		this.className = className;
	}

}
