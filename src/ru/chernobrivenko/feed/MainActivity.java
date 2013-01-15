package ru.chernobrivenko.feed;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

public class MainActivity extends Activity {
	IotdHandler iotdHandler;
	Handler handler;
	Bitmap image;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		handler = new Handler();
		refreshFromFeed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public class IotdHandler extends DefaultHandler {
		private String url = "http://www.nasa.gov/rss/image_of_the_day.rss";
		private boolean inUrl = false;
		private boolean inTitle = false;
		private boolean inDescription = false;
		private boolean inItem = false;
		private boolean inDate = false;
		private Bitmap image = null;
		private String title = null;
		private StringBuffer description = new StringBuffer();
		private String date = null;


		public void processFeed() {
			try {
				SAXParserFactory factory =
						SAXParserFactory.newInstance();
				SAXParser parser = factory.newSAXParser();
				XMLReader reader = parser.getXMLReader();
				reader.setContentHandler(this);
				InputStream inputStream = new URL(url).openStream();
				reader.parse(new InputSource(inputStream));
			} catch (Exception e) {  }
		}

		private Bitmap getBitmap(String url) {
			try {
				HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
				connection.setDoInput(true);
				connection.connect();
				InputStream input = connection.getInputStream();
				Bitmap bilde = BitmapFactory.decodeStream(input);
				input.close();
				return bilde;
			} catch (IOException ioe) { return null; }
		}

		public void startElement(String url, String localName, String qName, Attributes attributes) throws SAXException {
			if (localName.equals("enclosure"))
			{
				inUrl = true;
				image = getBitmap(attributes.getValue("url"));
			}
			else { inUrl = false; }

			if (localName.startsWith("item")) { inItem = true; }
			else if (inItem) {

				if (localName.equals("title")) { inTitle = true; }
				else { inTitle = false; }

				if (localName.equals("description")) { inDescription = true; }
				else { inDescription = false; }

				if (localName.equals("pubDate")) { inDate = true; }
				else { inDate = false; }
			}
		}


		public void characters(char ch[], int start, int length) 
		{
			String chars = new String(ch).substring(start, start + length);

			if (inTitle && title == null) { title = chars; }
			if (inDescription) { description.append(chars); }
			if (inDate && date == null) { date = chars; }


		}

		public Bitmap getImage() { return image; }
		public String getTitle() { return title; }
		public StringBuffer getDescription() { return description; }
		public String getDate() { return date; }


	}

	private void resetDisplay (String title, String date, Bitmap image, StringBuffer description) {


		TextView titleView = (TextView) findViewById (R.id.imageTitle);
		titleView.setText(title);

		TextView dateView = (TextView) findViewById(R.id.imageDate);
		dateView.setText(date);

		ImageView imageView = (ImageView) findViewById (R.id.imageDisplay);
		imageView.setImageBitmap(image);


		TextView descriptionView = (TextView) findViewById (R.id.imageDescription);
		descriptionView.setText(description);
	}


	private void refreshFromFeed()
	{
		final ProgressDialog dialog = ProgressDialog.show(this, "Loading", "Loading image");

		Thread th = new Thread(){
			public void run()
			{

				if(iotdHandler == null)
					iotdHandler = new IotdHandler ();
				
				iotdHandler.processFeed();
				image = iotdHandler.getImage();
				handler.post(new Runnable() {
					public void  run()
					{
						resetDisplay (iotdHandler.getTitle(), iotdHandler.getDate(),
								iotdHandler.getImage(), iotdHandler.getDescription());
						dialog.dismiss();
					}
				});
			}
		};

		th.start();
	}

	public void onRefresh(View view)
	{
		refreshFromFeed();
	}

	
	public void onSetWallpaper(View view)
	{
		Thread th = new Thread()
		{
			public void run()
			{
				WallpaperManager wlpMng = WallpaperManager.getInstance(MainActivity.this);
				
				try
				{
					wlpMng.setBitmap(image);
					handler.post(new Runnable()
					{
						public void run()
						{
							Toast.makeText(MainActivity.this,"Wallpaper set",Toast.LENGTH_SHORT).show();
						}
					}
					);
					
				}catch(Exception e)
				{
					e.printStackTrace();
					handler.post(new Runnable()
					{
						public void run()
						{
							Toast.makeText(MainActivity.this,"Error setting wallpaper",Toast.LENGTH_SHORT).show();
						}
					}
					);
				}
				
			}
		};
		th.start();
	}
}

