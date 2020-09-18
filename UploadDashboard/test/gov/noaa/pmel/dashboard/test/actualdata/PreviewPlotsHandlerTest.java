package gov.noaa.pmel.dashboard.test.actualdata;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import gov.noaa.pmel.dashboard.client.DatasetPreviewProfilePage;
import gov.noaa.pmel.dashboard.handlers.PreviewPlotsHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;

/**
 * Test of generating the preview plots, which also tests the 
 * complete process of generating the DSG files.  Uses an 
 * existing UploadDashboard installation with user-provided
 * data and metadata for the cruise given in EXPOCODE.
 * 
 * @author Karl Smith
 */
public class PreviewPlotsHandlerTest {

	private static final String EXPOCODE = "33RO20150822";

	@Test
	public void testCreatePreviewPlots() throws IOException {
		System.setProperty("CATALINA_BASE", System.getenv("HOME"));
		System.setProperty("UPLOAD_DASHBOARD_SERVER_NAME", "OAPUploadDashboard");
		final String timetag = "testing";
		DashboardConfigStore configStore = DashboardConfigStore.get(false);
		PreviewPlotsHandler plotsHandler = configStore.getPreviewPlotsHandler();
		File dsgFilesDir = plotsHandler.getDatasetPreviewDsgDir(EXPOCODE);
		File plotsDir = plotsHandler.getDatasetPreviewPlotsDir(EXPOCODE);

		plotsHandler.createPreviewPlots(EXPOCODE, timetag);

		File dsgFile = new File(dsgFilesDir, EXPOCODE + "_" + timetag + ".nc");
		assertTrue( dsgFile.exists() );
		dsgFile.delete();

		File plotFile;
		for ( String imgName : new String[] {
				DatasetPreviewProfilePage.LAT_VS_LON_IMAGE_NAME,
				DatasetPreviewProfilePage.LAT_LON_IMAGE_NAME,
				DatasetPreviewProfilePage.SAMPLE_VS_TIME_IMAGE_NAME } ) {
			plotFile = new File(plotsDir, EXPOCODE + "_" + imgName + "_" + timetag + ".gif");
			assertTrue( "Plot for " + imgName + " does not exist", plotFile.exists() );
			plotFile.delete();
		}
	}

}
