package mrmathami.cia.java.jdt.gephi.model;

public class EdgeGephi {
	private final int id;
	private final int source;
	private final int target;

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
				">" +
				"\n\t\t\t" + "<viz:color r=\"102\" g=\"102\" b=\"102\"></viz:color>" +
				"\n\t\t</edge>";
	}
}
