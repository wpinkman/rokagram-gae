package com.rokagram.entities;

import java.util.Date;

import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.utils.SystemProperty;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.OnSave;
import com.rokagram.backend.Utils;

@Cache
@Entity
public class TemplateEntity {
	@Id
	private String name;
	private String version;

	private Text html;

	@OnSave
	void onSave() {

		if (this.version == null) {
			String v = createCurrentVersion();
			this.version = v;
		}
	}

	private String createCurrentVersion() {
		String v = null;
		if (Utils.isDevServer()) {
			long now = new Date().getTime();
			long everyten = now / 1000 / 10;
			v = Long.toString(everyten);
		} else {
			v = SystemProperty.applicationVersion.get();
		}
		return v;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isCurrent() {
		String current = createCurrentVersion();
		boolean iscurrent = org.apache.commons.lang.StringUtils.equals(this.version, current);

		return iscurrent;
	}

	public void setHtml(Text html) {
		this.html = html;
	}

	public Text getHtml() {
		return html;
	}

	public String getTemplate() {
		String ret = null;
		if (this.html != null) {
			ret = this.html.getValue();
		}
		return ret;
	}

	public void setTemplate(String html) {
		this.html = new Text(html);
	}

}
