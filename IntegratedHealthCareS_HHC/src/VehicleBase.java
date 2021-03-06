
public class VehicleBase {
	private final int id;
	private final int maxCapacity; // passenger
	private final int quantity;
	
	public VehicleBase(int id, int quantity, int maxCapacity) {
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
