package info.blockchain.merchant.directory;
 
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class FetchData {

	public static String getURL(String URL) throws Exception {
		
		final int DefaultRequestRetry = 2;
		final int DefaultRequestTimeout = 60000;

		URL url = new URL(URL);

		String error = null;
		
		for (int i = 0; i < DefaultRequestRetry; i++) {

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			try {
				connection.setRequestMethod("GET");
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

				connection.setConnectTimeout(DefaultRequestTimeout);
				connection.setReadTimeout(DefaultRequestTimeout);

				connection.setInstanceFollowRedirects(false);

				connection.connect();

				if (connection.getResponseCode() == 200)
					return IOUtils.toString(connection.getInputStream(), "UTF-8");
				else
					error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
				
				Thread.sleep(5000);
			} finally {
				connection.disconnect();
			}
		}
		
		return error;
	}

}
