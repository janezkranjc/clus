package clus.io;

import jeans.util.*;

import clus.main.*;

import java.io.*;
import java.util.zip.ZipInputStream;

public class ClusReader {

	String m_Name;
	int m_Row = 0, m_Attr = 0, m_LastChar = 0;
	MStreamTokenizer m_Token;
	StringBuffer m_Scratch = new StringBuffer();	
	
	public ClusReader(String fname) throws IOException {
		m_Name = fname;
		open();
	}
	
	public String getName() {
		return m_Name;
	}
	
	public int getRow() {
		return m_Row;
	}
	
	public int countRows() throws IOException {
		int nbr = countRows2();
		reOpen();
		return nbr;
	}

	public void open() throws IOException {
		if (FileUtil.fileExists(m_Name)) {
			m_Token = new MStreamTokenizer(Settings.getFileAbsolute(m_Name));				
		} else {
			ZipInputStream zip = new ZipInputStream(new FileInputStream(m_Name+".zip"));
			zip.getNextEntry();
			m_Token = new MStreamTokenizer(zip);				
		}
		m_Token.setCommentChar('%');		
	}

	public void reOpen() throws IOException {
		m_Token.close();	
		open();
	}

	public void close() throws IOException {
		m_Token.close();
	}	
	
	public MStreamTokenizer getTokens() {
		return m_Token;
	}
	
	public boolean hasMoreTokens() throws IOException {
		Reader reader = m_Token.getReader();
		int ch = reader.read();
		setLastChar(ch);
		return ch != -1;		
	}
	
	public boolean isEol() throws IOException {
		Reader reader = m_Token.getReader();	
		int ch = getNextChar(reader);
		if (ch == 10 || ch == 13) return true;
		setLastChar(ch);
		return false;
	}
	
	public void setLastChar(int ch) {
		m_LastChar = ch;
	}
	
	public int getNextChar(Reader reader) throws IOException {
		if (m_LastChar != 0) {
			int ch = m_LastChar;
			m_LastChar = 0;
			return ch;
		}
		return reader.read();		
	}
	
	public void readEol() throws IOException {
		boolean allowall = false;
		Reader reader = m_Token.getReader();
		int ch = getNextChar(reader);
		while (ch != -1) {
			if (ch == 10 || ch == 13) {
				m_Attr = 0;
				m_Row++;
				break;
			} else if (ch == '%') {
				allowall = true;
			} else if (ch != ' ' && ch != '\t' && allowall == false) {
				throw new IOException("Too many data on row "+m_Row+": '"+(char)ch+"'");
			}
			ch = reader.read();
		}		
	}
	
	public void readTillEol() throws IOException {
		Reader reader = m_Token.getReader();
		int ch = getNextChar(reader);
		while (ch != -1) {
			if (ch == 10 || ch == 13) {
				setLastChar(13);
				break;
			}
			ch = reader.read();
		}
	}
	
	public String readString() throws IOException {
		int nb = 0;
		Reader reader = m_Token.getReader();
		m_Scratch.setLength(0);
		int ch = getNextChar(reader);
		while (ch != -1 && ch != ',') {
			if (ch == '%') {
				readTillEol();
				break;
			}
			if (ch != '\t' && ch != 10 && ch != 13) {
				m_Scratch.append((char)ch);
				if (ch != ' ') nb++; 
			} else {
				if (ch == 10 || ch == 13) setLastChar(13);
				if (nb > 0) break;
			}
			ch = reader.read();
		}
		String result = m_Scratch.toString().trim();
		if (result.length() > 0) {
			return result;
		} else {
			throw new IOException("Error reading attirbute "+m_Attr+" at row "+(m_Row+1));
		}
	}	
	
	public double readFloat() throws IOException {		
		int nb = 0;
		Reader reader = m_Token.getReader();
		m_Scratch.setLength(0);
		int ch = getNextChar(reader);
		while (ch != -1 && ch != ',') {
			if (ch != ' ' && ch != '\t' && ch != 10 && ch != 13) {
				m_Scratch.append((char)ch);
				nb++; 
			} else {
				if (ch == 10 || ch == 13) setLastChar(13);
				if (nb > 0) break;
			}
			ch = reader.read();
		}
		if (m_Scratch.length() > 0) {
			m_Attr++;
			String value = m_Scratch.toString();			
			try {	
				if (value.equals("?")) return Double.POSITIVE_INFINITY;			
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				throw new IOException("Error parsing numeric value '"+value+"' for attribute "+m_Attr+" at row "+(m_Row+1));
			}		
		} else {
			throw new IOException("Error reading numeric attirbute "+m_Attr+" at row "+(m_Row+1));
		}
	}
	
	public void skipTillComma() throws IOException {		
		int nb = 0;
		Reader reader = m_Token.getReader();
		int ch = getNextChar(reader);
		while (ch != -1 && ch != ',') {
			if (ch != ' ' && ch != '\t' && ch != 10 && ch != 13) {
				nb++; 
			} else {
				if (ch == 10 || ch == 13) setLastChar(13);
				if (nb > 0) break;
			}
			ch = reader.read();
		}
	}

	public int countRows2() throws IOException {
		int nbrows = 0;
		int nbchars = 0;
		Reader reader = m_Token.getReader();
		int ch = reader.read();
		while (ch != -1) {
			if (ch == 10 || ch == 13) {
				if (nbchars > 0) nbrows++;
				nbchars = 0;
			} else if (ch != ' ' && ch != '\t') {
				nbchars++;
			}
			ch = reader.read();
		}
		if (nbrows == 0) throw new IOException("Empty @data section in ARFF file");
		return nbrows;
	}
}
