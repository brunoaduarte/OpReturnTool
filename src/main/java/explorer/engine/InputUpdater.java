package explorer.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import explorer.google.SheetsServiceUtil;

public class InputUpdater {
	
	@SuppressWarnings("rawtypes")
	public static void updateInputResource() throws IOException, GeneralSecurityException, URISyntaxException {
		
		final String SPREADSHEET_ID = "";
		final String range = "input-txt!A1:E";
		Sheets sheetsService = SheetsServiceUtil.getSheetsService();
        ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
        List<List<Object>> values = response.getValues();

        URL input = MainEngine.class.getClassLoader().getResource("input.txt");
        if(input != null) {
        	File file = new File(input.getFile());
            file.delete();
            file.createNewFile();
            
        	FileOutputStream fos = new FileOutputStream(file);
        	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
         
            if (values == null || values.isEmpty()) {
                System.out.println("No data found.");
            } else {
                for (List row : values) {
                    String str = String.format("%s %s %s %s %s\r\n", row.get(0), row.get(1), row.get(2), row.get(3), row.get(4));
//                    System.out.printf(str);
                    bw.write(str);
                }
            }
         
        	bw.close();
            
        }
        
	}

}
