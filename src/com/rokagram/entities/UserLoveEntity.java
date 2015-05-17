package com.rokagram.entities;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class UserLoveEntity {

	@Id
	private String username;

	private int width;
	private int height;
	private String format;
	private Blob imageBlob;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Blob getImageBlob() {
		return imageBlob;
	}

	public void setImageBlob(Blob imageBlob) {
		this.imageBlob = imageBlob;
	}

	public byte[] getImageData() {
		byte[] ret = null;
		if (this.imageBlob != null) {
			ret = imageBlob.getBytes();
		}
		return ret;
	}

	public void setImageBlob(byte[] imageData) {
		if (imageData != null) {
			this.imageBlob = new Blob(imageData);
		} else {
			this.imageBlob = null;
		}
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getSrc() {
		String src = "data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAABGdBTUEAALGP C/xhBQAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9YGARc5KB0XV+IA AAAddEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIFRoZSBHSU1Q72QlbgAAAF1J REFUGNO9zL0NglAAxPEfdLTs4BZM4DIO4C7OwQg2JoQ9LE1exdlYvBBeZ7jq ch9//q1uH4TLzw4d6+ErXMMcXuHWxId3KOETnnXXV6MJpcq2MLaI97CER3N0 vr4MkhoXe0rZigAAAABJRU5ErkJggg==";
		if (this.imageBlob != null) {
			byte[] ecodedImageBytes = org.apache.commons.codec.binary.Base64.encodeBase64(this.getImageData());
			String encoded = new String(ecodedImageBytes);

			src = "data:image/" + this.format.toLowerCase() + ";base64, " + encoded;
		}
		return src;
	}

	@Override
	public String toString() {
		return "UserLoveEntity [username=" + username + ", width=" + width + ", height=" + height + ", format="
				+ format + "]";
	}

}
