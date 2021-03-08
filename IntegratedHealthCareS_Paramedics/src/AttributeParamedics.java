
public class AttributeParamedics {
	private final int id;
	private final int maxCapacity;
	private final int quantity;
	
	public AttributeParamedics(int id, int quantity, int maxCapacity) {
		this.id = id;
		this.maxCapacity = maxCapacity;
		this.quantity = quantity;
	}

	
	public int getQuantity() {
		return quantity;
	}


	public int getId() {
		return id;
	}

	public int getMaxCapacity() {
		return maxCapacity;
	}
	
	
}

