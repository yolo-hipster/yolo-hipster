package mo.umac.weha.diff.engine;

import java.util.ArrayList;
import java.util.List;

public class Tuple<T> {
	private ArrayList<T> objects;
	
	public Tuple(List<T> objects, int index, int size) {
		this.objects = new ArrayList<T>(size);
		for(int i = index; i < index + size; i++) {
			this.objects.add(objects.get(i));
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((objects == null) ? 0 : objects.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tuple<?> other = (Tuple<?>) obj;
		if (objects == null) {
			if (other.objects != null)
				return false;
		} else if (!objects.equals(other.objects))
			return false;
		return true;
	}

}
