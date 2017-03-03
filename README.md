#Ti.Ftp4j

This is the Titanium version of FTP client [ftp4j](http://www.sauronsoftware.it/projects/ftp4j/index.php)

 __The ftp4j library implements a Java full-features FTP client. With ftp4j embedded in your application you can: transfer files (upload and download), browse the remote FTP site (directory listing included), create, delete, rename and move remote directories and files.__


##Usage

```javascript

var FTP = require("de.appwerft.ftp4j");

var client = FTP.createFTPclient();
client.connect("ftp.org",21);
client.addEventListener("connected",function(){
	client.login("login","password");
});
client.addEventListener("logincomplete",function(){
	client.disconnect(false);
});



```