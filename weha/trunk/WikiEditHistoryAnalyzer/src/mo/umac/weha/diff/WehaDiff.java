package mo.umac.weha.diff;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.diff.formatter.WehaDiffTableFormatter;
import mo.umac.weha.diff.paragraph.ParagraphDiff;
import mo.umac.weha.diff.paragraph.ParagraphEdit;
import mo.umac.weha.lexer.ParagraphSplitter;

public class WehaDiff {
	
	public static void main(String[] args) {
		String oldFilename = null;
		String newFilename = null;
		
		if (args.length == 2) {
			oldFilename = args[0];
			newFilename = args[1];
		}
		else {
			System.err.println("Usage: WehaDiff oldText.txt newText.txt");
			System.exit(1);
		}
		
		BufferedReader oldReader = null;
		BufferedReader newReader = null;
		try {
			oldReader = new BufferedReader( new InputStreamReader(
					new FileInputStream(oldFilename), "UTF8") );
			newReader = new BufferedReader( new InputStreamReader(
					new FileInputStream(newFilename), "UTF8")  );
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		String tmp;
		StringBuilder oldString = new StringBuilder();
		StringBuilder newString = new StringBuilder();
		
		try {
			while ( (tmp = oldReader.readLine()) != null) {
				oldString.append(tmp);
				oldString.append("\n");
			}
			while ( (tmp = newReader.readLine()) != null) {
				newString.append(tmp);
				newString.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List<Paragraph> oldParas = ParagraphSplitter.split(oldString.toString());
		List<Paragraph> newParas = ParagraphSplitter.split(newString.toString());
		
		List<ParagraphEdit> paraDiff = ParagraphDiff.diff(oldParas, newParas);
		
		String formattedDiff = WehaDiffTableFormatter.formatDiff(paraDiff);
		
		System.out.println(formattedDiff);
	}
	
}
