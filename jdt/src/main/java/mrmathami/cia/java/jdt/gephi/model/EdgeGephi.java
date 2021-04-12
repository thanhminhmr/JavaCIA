package mrmathami.cia.java.jdt.gephi.model;

public class EdgeGephi {
	private int id;
	private int source;
	private int target;

	public EdgeGephi(int id, int source, int target) {
		this.id = id;
		this.source = source;
		this.target = target;
	}

	@Override
	public String toString() {
		return "<edge " +
				"id='" + id + '\'' +
				" source='" + source + '\'' +
				" target='" + target + '\'' +
				"></edge>";
	}
}
