package clus.selection;

public class XValGroup {

	protected int[] m_Elements;
	protected int m_NbElements;
	
	public XValGroup(int max) {
		m_Elements = new int[max];
	}

	public boolean add(int which, int max) {
		if (m_NbElements >= max) return false;
		m_Elements[m_NbElements++] = which;
		return true;
	}

	public int getNbElements() {
		return m_NbElements;
	}
	
	public int getElement(int idx) {
		return m_Elements[idx];
	}
	
	public void print() {
		System.out.print("[");
		for (int i = 0; i < getNbElements(); i++) {
			if (i != 0) System.out.print(",");
			System.out.print(getElement(i));
		}
		System.out.println("]");		
	}
}
