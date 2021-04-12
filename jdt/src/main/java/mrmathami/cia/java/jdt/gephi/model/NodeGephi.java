package mrmathami.cia.java.jdt.gephi.model;

import org.eclipse.text.edits.DeleteEdit;

public class NodeGephi {
	public enum Type {
		UNCHANGE(0, 153, 153),
		DELETE(255, 0 , 0),
		CHANGE(255, 255, 0),
		ADD(0, 255, 153);

		private int r;
		private int g;
		private int b;

		Type(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
	private final int id;
	private final String label;
	private final String size = "<viz:size value='4.0'></viz:size>";
	private final Type type;
	public NodeGephi(int id, String label, Type type) {
		this.id = id;
		this.label = label;
		this.type = type;
	}

	@Override
	public String toString() {
		return "<node " +
				"id='" + id + '\'' +
				" label='" + label + '\'' +
				'>' +
				"\n\t\t\t" + size +
				toStringType(type) +
				"\n\t\t</node>";
	}

	private String toStringType(Type type) {
		return "\n\t\t\t<viz:color" +
				" r='" + type.r + '\'' +
				" g='" + type.g + '\'' +
				" b='" + type.b + '\'' +
				"></viz:color>";
	}
}
