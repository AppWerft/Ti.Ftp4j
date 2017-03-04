package de.appwerft.ftp4j;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFile;
import org.appcelerator.titanium.io.TiFileFactory;

import android.os.AsyncTask;

public class FTPclient {
	private static final String LCAT = "FTP4j";
	private String host;
	private String login = "anonymous";
	private String password = "ftp4j";
	private int port = 0;
	private TiBlob responseData;
	private OutputStream responseOut;
	private String fullPath = "/";
	private String directory = null;
	private String fileName = null;
	private String mtime;
	private long fsize;

	private TiFile responseFile;
	private KrollFunction onLoad;
	private KrollFunction onError;
	private KrollFunction onProgress;
	private KrollProxy proxy;
	FTPClient client;

	public FTPclient(UploadProxy proxy) {
		KrollDict opts = proxy.opts;
		this.proxy = proxy;
		this.client = proxy.client;
		client(opts);
	}

	public FTPclient(DownloadProxy proxy) {
		KrollDict opts = proxy.opts;
		this.proxy = proxy;
		this.client = proxy.client;
		client(opts);
	}

	void client(KrollDict opts) {
		if (opts.containsKeyAndNotNull(TiC.PROPERTY_ONLOAD)) {
			Object o = opts.get(TiC.PROPERTY_ONLOAD);
			if (o instanceof KrollFunction) {
				onLoad = (KrollFunction) o;
			}
		}
		if (opts.containsKeyAndNotNull("onprogress")) {
			Object o = opts.get("onprogress");
			if (o instanceof KrollFunction) {
				onProgress = (KrollFunction) o;
			}
		}
		if (opts.containsKeyAndNotNull(TiC.PROPERTY_ONERROR)) {
			Object o = opts.get(TiC.PROPERTY_ONERROR);
			if (o instanceof KrollFunction) {
				onError = (KrollFunction) o;
			}
		}
		if (opts.containsKeyAndNotNull(TiC.PROPERTY_FILE)) {
			Object f = opts.get(TiC.PROPERTY_FILE);
			Log.d(LCAT, f.getClass().getSimpleName());
			if (f instanceof String) {
				String fileName = (String) f;
				TiBaseFile baseFile = TiFileFactory.createTitaniumFile(
						fileName, false);
				if (baseFile instanceof TiFile) {
					responseFile = (TiFile) baseFile;
				} else
					Log.w(LCAT, "no instanceof TiFile");
			}

			if (responseFile == null) {
				Log.w(LCAT,
						"Ignore the provided response file because it is not valid / writable.");
			}
		}
		if (opts.containsKeyAndNotNull(TiC.PROPERTY_URL)) {
			try {
				URL url = new URL(opts.getString(TiC.PROPERTY_URL));
				host = url.getHost();
				String user;
				user = url.getUserInfo();
				if (user != null && user.contains(":")) {
					login = user.split(":")[0];
					password = user.split(":")[1];
				}
				port = url.getPort();
				fullPath = url.getPath();
			} catch (MalformedURLException e) {
				Log.e(LCAT,
						"wrong format url " + opts.getString(TiC.PROPERTY_URL));
				e.printStackTrace();
				sendError("malformedIURL", 0);
			}
		}
		String[] parts = fullPath.split("/");
		if (parts[parts.length - 1].equals("")) {
			/* pure path without file */
			directory = fullPath;
		} else {
			fileName = parts[parts.length - 1];
			directory = fullPath.substring(0,
					fullPath.length() - fileName.length());
		}
		(new FTPsessionConnect()).execute();
	}

	protected void sendError(String message, int number) {
		if (onError != null) {
			KrollDict kd = new KrollDict();
			kd.put("message", message);
			kd.put("error", number);
			onError.call(proxy.getKrollObject(), kd);
		}

	}

	private class FTPsessionConnect extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			KrollDict kd = new KrollDict();
			try {
				if (port != 0)
					client.connect(host, port);
				else
					client.connect(host);
			} catch (IllegalStateException | IOException
					| FTPIllegalReplyException | FTPException e1) {
				e1.printStackTrace();
				sendError(e1.getMessage(), 0);
				return "E";
			}
			try {
				client.login(login, password);
			} catch (IllegalStateException | IOException
					| FTPIllegalReplyException | FTPException e1) {
				e1.printStackTrace();
				sendError(e1.getMessage(), 0);
				return "E";
			}
			if (!client.isConnected())
				return "";
			try {
				client.changeDirectory("." + directory);
			} catch (IllegalStateException | IOException
					| FTPIllegalReplyException | FTPException e1) {
				e1.printStackTrace();
				sendError(e1.getMessage(), 0);
				return "E";
			}
			String[] fileList = null;
			try {
				try {
					FTPFile[] list = client.list();
					for (FTPFile item : list) {
						fsize = item.getSize();
						kd.put("size", fsize);
						kd.put("mtime", item.getModifiedDate());
					}
				} catch (IllegalStateException | IOException
						| FTPIllegalReplyException | FTPException e1) {
					e1.printStackTrace();
				}
			} catch (FTPDataTransferException | FTPAbortedException
					| FTPListParseException e) {
				e.printStackTrace();
				sendError(e.getMessage(), 0);
			}
			kd.put("fileNames", fileList);
			if (fileName == null && onLoad != null) {
				onLoad.call(proxy.getKrollObject(), kd);
				try {
					client.disconnect(false);
				} catch (IllegalStateException | IOException
						| FTPIllegalReplyException | FTPException e) {
					e.printStackTrace();
				}
				return "";
			}
			if (fileName != null) {
				try {
					FTPFile[] list = client.list(fileName);
					for (FTPFile item : list) {
						fsize = item.getSize();
						kd.put("size", fsize);
						kd.put("mtime", item.getModifiedDate());
					}
					if (onLoad != null)
						onLoad.call(proxy.getKrollObject(), kd);
				} catch (IllegalStateException | IOException
						| FTPIllegalReplyException | FTPException
						| FTPDataTransferException | FTPAbortedException
						| FTPListParseException e1) {
					e1.printStackTrace();
					sendError(e1.getMessage(), 0);
					return "";
				}

				try {
					if (responseFile != null) {
						Log.d(LCAT, responseFile.getNativeFile()
								.getAbsolutePath());
						client.download(fileName, responseFile.getNativeFile()
								.getAbsoluteFile(), new MyTransferListener());
					} else
						Log.w(LCAT, "no file for download");
				} catch (IllegalStateException | IOException
						| FTPIllegalReplyException | FTPException
						| FTPDataTransferException | FTPAbortedException e) {
					e.printStackTrace();
					sendError(e.getMessage(), 0);
				}
			}
			return "Executed";
		}

	}

	public class MyTransferListener implements FTPDataTransferListener {
		double transferred = 0L;

		public void started() {
			Log.d(LCAT, ">>>>>>>>>>>>>>>>>>> Transfer started");
		}

		public void transferred(int length) {
			KrollDict kd = new KrollDict();
			kd.put("total", fsize);
			transferred += length;
			kd.put("transferred", transferred);
			kd.put("progress", ((double) transferred) / ((double) fsize));
			if (onProgress != null)
				onProgress.call(proxy.getKrollObject(), kd);
			else
				Log.w(LCAT, "onprogess was null");
			// Yet other length bytes has been transferred since the last time
			// this
			// method was called
		}

		public void completed() {
			Log.d(LCAT, ">>>>>>>>>>>>>>>>>>>>>>>>Transfer complete");
			// Transfer completed
		}

		public void aborted() {
			// Transfer aborted
		}

		public void failed() {
			Log.d(LCAT, ">>>>>>>>>>>>>>>>>>>>>>>>Transfer failed");
			// Transfer failed
		}

	}

}
