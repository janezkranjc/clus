package clus.ext.sspd;

import jeans.math.matrix.*;

import clus.io.*;

import java.io.*;

/*

vraagje van saso

- use a slightly changed version of Clus
  that would take as input vectors of PCPs + distance matrix
  over samples (rather than calculating the distance based
  on a vector of target variables):
  could we count on Jan Struyfs prodigal programming skills to make this change?

samengevat: afstanden tussen vbn worden 1x op voorhand berekend en in een
matrix gezet.  clustertrees worden gebouwd zuiver op basis van
paarsgewijze afstanden tussen vbn (er worden dus geen prototypes berekend).
Dit houdt in dat variantie vervangen wordt door bv. SSPD, Sum of Squared
Pairwise Distances (tussen 2 vbn. ipv tussen 1 vb en prototype), wat
equivalent moet zijn.

Dgl. bomen leveren in eerste instantie dan geen prototype op en kunnen dus
niet voor predictie gebruikt worden.  (soit, achteraf kan je prototypes dan
nog altijd gaan definieren maar da's een andere zaak, nu niet direct te
bekijken)

Kan je zo'n aanpassing in Clus voorzien?

Dank,
Hendrik

PS ik weet niet of ik je dit nu algezegd had, maar patent-plannen zijn
opgeborgen.  Ga maar voor snelle publicatie van hierarchical multi-class
trees!
*/

import clus.main.*;

public class SSPDMatrix extends MSymMatrix implements Serializable {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public SSPDMatrix(int size) {
		super(size, true);	
	}

	// Matrix stores squared distances [sum of (i,j)^2 and (j,i)^2]
	public static SSPDMatrix read(String filename, Settings sett) throws IOException {
		ClusReader reader = new ClusReader(filename, sett);
		int nb = 0;
		while (!reader.isEol()) {
			reader.readFloat();
			nb++;
		}
		System.out.println("Loading SSPD Matrix: "+filename+" (Size: "+nb+")");
		SSPDMatrix matrix = new SSPDMatrix(nb);
		reader.reOpen();
		for (int i = 0; i < nb; i++) {
			for (int j = 0; j < nb; j++) {
				double value = reader.readFloat();
				matrix.add_sym(i,j,value* value);
			}		
			if (!reader.isEol()) throw new IOException("SSPD Matrix is not square");
		}
		reader.close();	
//		matrix.print(ClusFormat.OUT_WRITER, ClusFormat.TWO_AFTER_DOT, 5);
//		ClusFormat.OUT_WRITER.flush();
		return matrix;
	}
}
