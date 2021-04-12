package mrmathami.cia.java.jdt.gephi.model;

public class EdgeGephi {
	private String id;
	private String source;
	private String target;

	public EdgeGephi(String id, String source, String target) {
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
