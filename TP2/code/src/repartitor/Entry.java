package repartitor;

public class Entry<Key, Value> {
	
	Key key;
	Value value;
	
	public Entry(Key key, Value value) {
		this.key = key;
		this.value = value;
	}
	
	public Key getKey() {
		return key;
	}
	
	public Value getValue() {
		return value;
	}
	
	@Override
	public boolean equals(Object object) {
		return object instanceof Entry && key.equals(((Entry<?, ?>)object).key);
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
}
