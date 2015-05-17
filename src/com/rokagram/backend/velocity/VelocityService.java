package com.rokagram.backend.velocity;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.rokagram.backend.Utils;

public class VelocityService {
	public static final String VELOCITY_NAMESPACE = "velocity";
	public static final Logger log = Logger.getLogger(VelocityService.class.getName());
	static VelocityEngine engine = null;
	public static final String TEMPLATES_DIR = "WEB-INF/templates";

	private VelocityService() {
		init();
	}

	private static class VelocityTemplateServiceHolder {
		public static final VelocityService instance = new VelocityService();
	}

	public static VelocityService getInstance() {
		if (Utils.isDevServer()) {
			return new VelocityService();
		} else {
			return VelocityTemplateServiceHolder.instance;
		}
	}

	private void init() {
		engine = new VelocityEngine();

		engine.setProperty("resource.manager.logwhenfound", "true");

		engine.setProperty("resource.loader", "file,entity");
		engine.setProperty("file.resource.loader.class", "com.rokagram.backend.velocity.HrdResourceLoader");

		boolean cache = !Utils.isDevServer();

		engine.setProperty("file.resource.loader.cache", cache);

		engine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
				"org.apache.velocity.runtime.log.Log4JLogChute");
		engine.setProperty("runtime.log.logsystem.log4j.logger", log.getName());

		engine.setProperty("foreach.provide.scope.control", true);

		engine.init();

	}

	public void flushMemcach() {
		MemcacheService memcache = MemcacheServiceFactory.getMemcacheService(VELOCITY_NAMESPACE);

		log.warning("CLEARING MEMCACHE");
		memcache.clearAll();

	}

	public Template getTemplate(String template) {
		Date start = new Date();

		Template t = engine.getTemplate(template);
		Date now = new Date();
		long ms = now.getTime() - start.getTime();

		if (ms > 0) {

			String message = "Found template:" + template + " in " + ms + "ms";
			if (!template.startsWith("admin/")) {
				if (ms > 1000) {
					log.warning(message);
				}
			}
		}
		return t;

	}

	public int warmCache() {
		File dir = new File(TEMPLATES_DIR);
		int i = 0;
		String[] children = dir.list();
		if (children == null) {
			// Either dir does not exist or is not a directory
		} else {
			for (i = 0; i < children.length; i++) {
				// Get filename of file or directory
				String filename = children[i];
				if (filename.endsWith(".html")) {
					try {
						getTemplate(filename);
					} catch (ResourceNotFoundException ex) {
						log.warning("Could not load " + filename + " ex:" + ex);
					}
				}
			}
		}
		return i;
	}

	public String evaluate(String templateString, Map<String, Object> contextMap) {
		String ret = null;
		VelocityContext context = new VelocityContext();
		for (String k : contextMap.keySet()) {
			context.put(k, contextMap.get(k));
		}

		Writer writer = new StringWriter();
		engine.evaluate(context, writer, "velocity", templateString);

		ret = writer.toString();
		return ret;

	}
}
