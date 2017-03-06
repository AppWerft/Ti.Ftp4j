#Ti.Ftp4j <img src="http://resource.thaicreate.com/upload/tutorial/java-ftp-upload-01.jpg?v=1001" width=140 />

This is the Titanium version of FTP client [ftp4j](http://www.sauronsoftware.it/projects/ftp4j/index.php)

_The ftp4j library implements a Java full-features FTP client. With ftp4j embedded in your application you can: transfer files (upload and download), browse the remote FTP site (directory listing included), create, delete, rename and move remote directories and files._

A pure JS implementaion you can find in [Kosso's gist](https://gist.github.com/kosso/32b6dcaba0dbf9f9d7fb)

##Usage

###Download
```javascript
var FTP = require("de.appwerft.ftp4j");
var client = FTP.createDownload({
	url : "ftp://gds32025:cE***bY@ftp-outgoing2.dwd.de:21/gds/specials/radar/Radarfilm_WEB_DL.gif",
	/* you use file or filename (nativePath) */
	file : Ti.Filesystem.getFile( Ti.Filesystem.applicationCacheDirectory,"rainradar.gif");
	keepalive : false, // autodisconenct after download 
	onload : function(e) {
		console.log(e);
	},
	onerror: function(e) {
		console.log(e);
	},
	onprogress: function(e) {
		console.log(e);
	}
});
```
If the URL is a path (without file) you will get a list of file names. If you request a full path with file you will get additional the file as Blob.


###Upload
```javascript
var FTP = require("de.appwerft.ftp4j");
var client = FTP.createUpload({
	url : "ftp://gds32025:cEtPCZbY@ftp-outgoing2.dwd.de:21/gds/specials/radar/Radarfilm_WEB_DL.gif",
	file : Ti.Filesystem.getFile( Ti.Filesystem.applicationCacheDirectory,"rainradar.gif");
	keepalive : false, // autodisconenct after download 
	onload : function(e) {
		console.log(e);
	},
	onerror: function(e) {
		console.log(e);
	},
	onprogress: function(e) {
		console.log(e);
	}
});
```
If the URL is a path (without file) you will get a list of file names. If you request a full path with file you will get additional the file as Blob.

###Browsing the remote site

Change directory with:
```javascript
client.changeDirectory(newPath);
```
Back to the parent directory with:
```javascript
client.changeDirectoryUp();
```

###Listing files, directories and links

The FTP protocol doesn't offer a wide supported method to get complete informations about the contents of the working directory. The LIST command usually gives all you need to know, but unfortunately every server can use a different style for the response. It means that some servers return a UNIX style directory listing, some servers prefer the DOS style, others use some alternative ones.

The ftp4j library can handle many LIST response formats, building from them a unified structured object representation of the directory contents. Currently ftp4j can handle:

    UNIX style and variants (i.e. MAC style)
    DOS style
    NetWare styles
    EPLF
    MLSD

This is done using pluggable parsers. The package it.sauronsoftware.ftp4j.listparsers contains the ones handling the styles listed above. Most of the time this should be enough.

To list the current working directory entries call:
```javascript
var list = client.list();
list.forEach(function(file){
	console.log(file); // object with name, ctime, size etc.
});
```
Only the names (more stable)
```javascript
var list = client.listNames();
list.forEach(function(file){
	console.log(file); 
});
```
###Download

```