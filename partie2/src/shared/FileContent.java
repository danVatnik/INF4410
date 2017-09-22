package shared;

import java.io.UnsupportedEncodingException;

public class FileContent extends FileCommonInfo {
	private byte[] fileContent;
	
	public FileContent(String fileName, byte[] fileContent)
	{
		super(fileName);
		this.fileContent = fileContent;
		/*
		File fichierALire = new File(fileName);
		if(fichierALire.length() != 0 && fichierALire.length() < Integer.MAX_VALUE)
		{
			FileInputStream streamToReadFile = new FileInputStream(fichierALire);
			byte[] bytesRead = new byte[(int)fichierALire.length()];
			try
			{
				streamToReadFile.read(bytesRead);
			}
			finally
			{
				streamToReadFile.close();
			}
			fileContent = new String(bytesRead, "UTF-8");
		}
		else
		{
			throw new IOException("The file exceed the maximum size of " + Integer.MAX_VALUE + " bytes.");
		}
		*/
	}
	
	public String getFileContent() {
		try {
			return new String(fileContent, "UTF-8");
		}
		catch(UnsupportedEncodingException e) {
			return null;
		}
	}
}
