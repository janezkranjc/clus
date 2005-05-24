import clus.tools.debug.Debug;

import jeans.util.array.*;

public class TestInt {
	
	public final static int[] L1 = {1,5,6,7,20};
	public final static int[] L2 = {2,3,5,7,10,20};

	public static void main(String[] args) {
		System.out.println("L1 = "+MyIntArray.print(L1));
		System.out.println("L2 = "+MyIntArray.print(L2));		
		int[] r = MyIntArray.intersectSorted(L1, L2);
		System.out.println("L3 = "+MyIntArray.print(r));		
	}

}
