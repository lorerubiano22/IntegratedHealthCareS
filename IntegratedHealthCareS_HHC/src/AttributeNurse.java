
public class AttributeNurse {
	private final int id;
	private final int qualification;
	private final int quantity;

	public AttributeNurse(int id, int quantity, int qualif) {
		this.id = id;
		this.qualification = qualif;
		this.quantity = quantity;
	}

	public int getId() {
		return id;
	}

	public int getQualif() {
		return qualification;
	}

	public int getQuantity() {
		return quantity;
	}

	@Override
	public String toString() {
		return "Agent [id=" + id + ", qualif=" + qualification + ", quantity="
				+ quantity + "]";
	}


}
